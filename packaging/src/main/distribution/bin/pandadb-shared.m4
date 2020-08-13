#############################################################
#      File content is generated based on .m4 template      #
#############################################################

set -o errexit -o nounset -o pipefail
[[ "${TRACE:-}" ]] && set -o xtrace

declare -r PROGRAM="$(basename "$0")"

# Sets up the standard environment for running Pandadb shell scripts.
#
# Provides these environment variables:
#   PANDADB_HOME
#   PANDADB_CONF
#   PANDADB_DATA
#   PANDADB_LIB
#   PANDADB_LOGS
#   PANDADB_PIDFILE
#   PANDADB_PLUGINS
#   one per config setting, with dots converted to underscores
#
setup_environment() {
  _setup_calculated_paths
  _read_config
  _setup_configurable_paths
}

setup_heap() {
  if [[ -n "${HEAP_SIZE:-}" ]]; then
    JAVA_MEMORY_OPTS_XMS="-Xms${HEAP_SIZE}"
    JAVA_MEMORY_OPTS_XMX="-Xmx${HEAP_SIZE}"
  fi
}

build_classpath() {
  CLASSPATH="${PANDADB_PLUGINS}:${PANDADB_CONF}:${PANDADB_LIB}/*:${PANDADB_PLUGINS}/*"

  # augment with tools.jar, will need JDK
  if [ "${JAVA_HOME:-}" ]; then
    JAVA_TOOLS="${JAVA_HOME}/lib/tools.jar"
    if [[ -e $JAVA_TOOLS ]]; then
      CLASSPATH="${CLASSPATH}:${JAVA_TOOLS}"
    fi
  fi
}

detect_os() {
  if uname -s | grep -q Darwin; then
    DIST_OS="macosx"
  elif [[ -e /etc/gentoo-release ]]; then
    DIST_OS="gentoo"
  else
    DIST_OS="other"
  fi
}

setup_memory_opts() {
  # In some cases the heap size may have already been set before we get here, from e.g. HEAP_SIZE env.variable, if so then skip
  if [[ -n "${dbms_memory_heap_initial_size:-}" && -z "${JAVA_MEMORY_OPTS_XMS-}" ]]; then
    local mem="${dbms_memory_heap_initial_size}"
    if ! [[ ${mem} =~ .*[gGmMkK] ]]; then
      mem="${mem}m"
      cat >&2 <<EOF
WARNING: dbms.memory.heap.initial_size will require a unit suffix in a
         future version of Pandadb. Please add a unit suffix to your
         configuration. Example:

         dbms.memory.heap.initial_size=512m
                                          ^
EOF
    fi
    JAVA_MEMORY_OPTS_XMS="-Xms${mem}"
  fi
  # In some cases the heap size may have already been set before we get here, from e.g. HEAP_SIZE env.variable, if so then skip
  if [[ -n "${dbms_memory_heap_max_size:-}" && -z "${JAVA_MEMORY_OPTS_XMX-}" ]]; then
    local mem="${dbms_memory_heap_max_size}"
    if ! [[ ${mem} =~ .*[gGmMkK] ]]; then
      mem="${mem}m"
      cat >&2 <<EOF
WARNING: dbms.memory.heap.max_size will require a unit suffix in a
         future version of Pandadb. Please add a unit suffix to your
         configuration. Example:

         dbms.memory.heap.max_size=512m
                                      ^
EOF
    fi
    JAVA_MEMORY_OPTS_XMX="-Xmx${mem}"
  fi
}

check_java() {
  _find_java_cmd
  setup_memory_opts

  version_command=("${JAVA_CMD}" "-version" ${JAVA_MEMORY_OPTS_XMS-} ${JAVA_MEMORY_OPTS_XMX-})

  JAVA_VERSION=$("${version_command[@]}" 2>&1 | awk -F '"' '/version/ {print $2}')
  if [[ $JAVA_VERSION = "1."* ]]; then
    if [[ "${JAVA_VERSION}" < "1.8" ]]; then
      echo "ERROR! Pandadb cannot be started using java version ${JAVA_VERSION}. "
      _show_java_help
      exit 1
    fi
    if ! ("${version_command[@]}" 2>&1 | egrep -q "(Java HotSpot\\(TM\\)|OpenJDK|IBM) (64-Bit Server|Server|Client|J9) VM"); then
      unsupported_runtime_warning
    fi
  elif [[ $JAVA_VERSION = "11"* ]]; then
    if ! ("${version_command[@]}" 2>&1 | egrep -q "(Java HotSpot\\(TM\\)|OpenJDK|IBM) (64-Bit Server|Server|Client|J9) VM"); then
       unsupported_runtime_warning
    fi
  else
      unsupported_runtime_warning
  fi
}

unsupported_runtime_warning() {
    echo "WARNING! You are using an unsupported Java runtime. "
    _show_java_help
}

# Resolve a path relative to $PANDADB_HOME.  Don't resolve if
# the path is absolute.
resolve_path() {
    orig_filename=$1
    if [[ ${orig_filename} == /* ]]; then
        filename="${orig_filename}"
    else
        filename="${PANDADB_HOME}/${orig_filename}"
    fi
    echo "${filename}"
}

call_main_class() {
  setup_environment
  check_java
  build_classpath
  EXTRA_JVM_ARGUMENTS="-Dfile.encoding=UTF-8"
  class_name=$1
  shift

  export PANDADB_HOME PANDADB_CONF

  exec "${JAVA_CMD}" ${JAVA_OPTS:-} ${JAVA_MEMORY_OPTS_XMS-} ${JAVA_MEMORY_OPTS_XMX-} \
    -classpath "${CLASSPATH}" \
    ${EXTRA_JVM_ARGUMENTS:-} \
    $class_name "$@"
}

_find_java_cmd() {
  [[ "${JAVA_CMD:-}" ]] && return
  detect_os
  _find_java_home

  if [[ "${JAVA_HOME:-}" ]] ; then
    JAVA_CMD="${JAVA_HOME}/bin/java"
    if [[ ! -f "${JAVA_CMD}" ]]; then
      echo "ERROR: JAVA_HOME is incorrectly defined as ${JAVA_HOME} (the executable ${JAVA_CMD} does not exist)"
      exit 1
    fi
  else
    if [ "${DIST_OS}" != "macosx" ] ; then
      # Don't use default java on Darwin because it displays a misleading dialog box
      JAVA_CMD="$(which java || true)"
    fi
  fi

  if [[ ! "${JAVA_CMD:-}" ]]; then
    echo "ERROR: Unable to find Java executable."
    _show_java_help
    exit 1
  fi
}

_find_java_home() {
  [[ "${JAVA_HOME:-}" ]] && return

  case "${DIST_OS}" in
    "macosx")
      JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
      ;;
    "gentoo")
      JAVA_HOME="$(java-config --jre-home)"
      ;;
  esac
}

_show_java_help() {
  echo "* Please use Oracle(R) Java(TM) 8, OpenJDK(TM) or IBM J9 to run Pandadb."
  echo "* Please see https://pandadb.com/docs/ for Pandadb installation instructions."
}

_setup_calculated_paths() {
  if [[ -z "${PANDADB_HOME:-}" ]]; then
    PANDADB_HOME="$(cd "$(dirname "$0")"/.. && pwd)"
  fi
  : "${PANDADB_CONF:="${PANDADB_HOME}/conf"}"
  readonly PANDADB_HOME PANDADB_CONF
}

_read_config() {
  # - plain key-value pairs become environment variables
  # - keys have '.' chars changed to '_'
  # - keys of the form KEY.# (where # is a number) are concatenated into a single environment variable named KEY
  parse_line() {
    line="$1"
    if [[ "${line}" =~ ^([^#\s][^=]+)=(.+)$ ]]; then
      key="${BASH_REMATCH[1]//./_}"
      value="${BASH_REMATCH[2]}"
      if [[ "${key}" =~ ^(.*)_([0-9]+)$ ]]; then
        key="${BASH_REMATCH[1]}"
      fi
      # Ignore keys that start with a number because export ${key}= will fail - it is not valid for a bash env var to start with a digit
      if [[ ! "${key}" =~ ^[0-9]+.*$ ]]; then
        if [[ "${!key:-}" ]]; then
          export ${key}="${!key} ${value}"
        else
          export ${key}="${value}"
        fi
      else
        echo >&2 "WARNING: Ignoring key ${key}, environment variables cannot start with a number."
      fi
    fi
  }

  if [[ -f "${PANDADB_CONF}/pandadb-wrapper.conf" ]]; then
    cat >&2 <<EOF
WARNING: pandadb-wrapper.conf is deprecated and support for it will be removed in a future
         version of Pandadb; please move all your settings to pandadb.conf
EOF
  fi

  for file in "pandadb-wrapper.conf" "pandadb.conf"; do
    path="${PANDADB_CONF}/F:\pandadb-v0.2\packaging\pom.xml"
    if [ -e "${path}" ]; then
      while read line; do
        parse_line "${line}"
      done <"${path}"
    fi
  done
}

_setup_configurable_paths() {
  PANDADB_DATA=$(resolve_path "${dbms_directories_data:-data}")
  PANDADB_LIB=$(resolve_path "${dbms_directories_lib:-lib}")
  PANDADB_LOGS=$(resolve_path "${dbms_directories_logs:-logs}")
  PANDADB_PLUGINS=$(resolve_path "${dbms_directories_plugins:-plugins}")
  PANDADB_RUN=$(resolve_path "${dbms_directories_run:-run}")
  PANDADB_CERTS=$(resolve_path "${dbms_directories_certificates:-certificates}")

  if [ -z "${dbms_directories_import:-}" ]; then
    PANDADB_IMPORT="NOT SET"
  else
    PANDADB_IMPORT=$(resolve_path "${dbms_directories_import:-}")
  fi

  readonly PANDADB_DATA PANDADB_LIB PANDADB_LOGS PANDADB_PLUGINS PANDADB_RUN PANDADB_IMPORT PANDADB_CERTS
}

print_configurable_paths() {
  cat <<EOF
Directories in use:
  home:         ${PANDADB_HOME}
  config:       ${PANDADB_CONF}
  logs:         ${PANDADB_LOGS}
  plugins:      ${PANDADB_PLUGINS}
  import:       ${PANDADB_IMPORT}
  data:         ${PANDADB_DATA}
  certificates: ${PANDADB_CERTS}
  run:          ${PANDADB_RUN}
EOF
}

print_active_database() {
  echo "Active database: ${dbms_active_database:-graph.db}"
}
