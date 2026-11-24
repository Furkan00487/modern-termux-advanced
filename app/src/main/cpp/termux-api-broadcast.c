/* termux-api.c - helper binary for calling built-in Termux clipboard API
 * Usage: termux-clipboard-api Clipboard ${ADDITIONAL_FLAGS}
 * This executes
 *   am broadcast --user 0 -n com.termux/com.termux.api.TermuxApiReceiver \
 *   --es socket_input ${OUTPUT_SOCKET} \
 *   --es socket_output ${INPUT_SOCKET} \
 *   --es api_method Clipboard \
 *   ${ADDITIONAL_FLAGS}
 * where ${INPUT_SOCKET} and ${OUTPUT_SOCKET} are addresses to linux
 * abstract namespace sockets, used to pass on stdin to the java
 * implementation and pass back output from java to stdout.
 */

#include "termux-api.h"

int main(int argc, char** argv) {
    /* Run the api command */
    run_api_command(argc, argv);

    return 0;
}
