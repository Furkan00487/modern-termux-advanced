#define _POSIX_SOURCE
#define _GNU_SOURCE
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <time.h>
#include <unistd.h>

#include "termux-api.h"

#define TERMUX_API_PACKAGE_VERSION "0.60.0"

#ifndef PREFIX
# define PREFIX "/data/data/com.termux/files/usr"
#endif

_Noreturn void contact_plugin(int argc, char** argv,
                                 char* input_address_string,
                                 char* output_address_string) {
    pid_t api_server_pid = getppid(); // Assume running after fork, so use `getppid()` instead of `getpid()`.
    uid_t api_server_uid = getuid();
    unsigned long long api_server_starttime = get_process_starttime(api_server_pid);

    contact_plugin_v2(argc, argv,
        api_server_pid, api_server_uid, api_server_starttime,
        input_address_string, output_address_string);
}

/* Dispatch command via one-shot `am broadcast` without keeping an idle socket server running. */
_Noreturn void contact_plugin_v2(int argc, char** argv,
                                 pid_t api_server_pid,
                                 uid_t api_server_uid,
                                 unsigned long long api_server_starttime,
                                 char* input_address_string,
                                 char* output_address_string)
{
    exec_am_broadcast_v2(argc, argv,
        api_server_pid, api_server_uid, api_server_starttime,
        input_address_string, output_address_string);
}




_Noreturn void exec_am_broadcast(int argc, char** argv,
                                 char* input_address_string,
                                 char* output_address_string) {
    pid_t api_server_pid = getppid(); // Assume running after fork, so use `getppid()` instead of `getpid()`.
    uid_t api_server_uid = getuid();
    unsigned long long api_server_starttime = get_process_starttime(api_server_pid);

    exec_am_broadcast_v2(argc, argv,
        api_server_pid, api_server_uid, api_server_starttime,
        input_address_string, output_address_string);
}

static char* resolve_android_user_id(void) {
    const char *env_user_id = getenv("TERMUX__USER_ID");
    if (env_user_id != NULL && env_user_id[0] != '\0') {
        bool valid = true;
        for (size_t i = 0; env_user_id[i] != '\0'; i++) {
            if (env_user_id[i] < '0' || env_user_id[i] > '9') {
                valid = false;
                break;
            }
        }
        if (valid) {
            return (char *) env_user_id;
        }
    }
    return "0";
}

// Function which execs "am broadcast ..".
_Noreturn void exec_am_broadcast_v2(int argc, char** argv,
                                    pid_t api_server_pid,
                                    uid_t api_server_uid,
                                    unsigned long long api_server_starttime,
                                    char* input_address_string,
                                    char* output_address_string)
{
    // Redirect stdout to /dev/null (but leave stderr open):
    close(STDOUT_FILENO);
    open("/dev/null", O_RDONLY);
    // Close stdin:
    close(STDIN_FILENO);

    const int child_pre_argc = 23;
    const int child_post_argc = argc > 1 ? argc - 1 : 0; // Except `argv[0]`.
    const int child_argc = child_pre_argc + child_post_argc;

    size_t child_argv_size = (sizeof(char*)) * (child_argc + 1); // Including trailing `NULL`.
    // Do not directly cast, otherwise can trigger null pointer dereference if `NULL` is returned.
    void* result = malloc(child_argv_size);
    if (result == NULL) {
        perror("malloc failed for am child args");
        exit(1);
    }

    char **child_argv = (char **) result;

    child_argv[0] = "am";
    child_argv[1] = "broadcast";
    child_argv[2] = "--user";
    child_argv[3] = resolve_android_user_id();
    child_argv[4] = "-n";
    child_argv[5] = "com.termux/com.termux.api.TermuxApiReceiver";
    child_argv[6] = "--es";
    // Input/output are reversed for the java process (our output is its input):
    child_argv[7] = "socket_input";
    child_argv[8] = output_address_string;
    child_argv[9] = "--es";
    child_argv[10] = "socket_output";
    child_argv[11] = input_address_string;

    child_argv[12] = (char *) "--ei";
    child_argv[13] = (char *) "api_server_pid";
    if (asprintf(&child_argv[14], "%ld", (long) api_server_pid) == -1) {
        child_argv[14] = (char *) "-1";
    }

    child_argv[15] = (char *) "--ei";
    child_argv[16] = (char *) "api_server_uid";
    if (asprintf(&child_argv[17], "%ju", (uintmax_t) api_server_uid) == -1) {
        child_argv[17] = (char *) "-1";
    }

    child_argv[18] = (char *) "--ei";
    child_argv[19] = (char *) "api_server_starttime";
    if (asprintf(&child_argv[20], "%llu", api_server_starttime) == -1) {
        child_argv[20] = (char *) "-1";
    }

    child_argv[21] = "--es";
    child_argv[22] = "api_method";

    // Copy the remaining arguments except `argv[0]`, `argv[1]` should be `api_method` extra value:
    if (child_post_argc > 0) {
        memcpy(child_argv + child_pre_argc, argv + 1, (size_t) child_post_argc * sizeof(char*));
    }

    // End with NULL:
    child_argv[child_argc] = NULL;

    // Use an a executable taking care of PATH and LD_LIBRARY_PATH:
    execv(PREFIX "/bin/am", child_argv);

    // We should not reach here, if we do, then free memory we malloc'ed
    free(child_argv);
    perror("execv(\"" PREFIX "/bin/am\")");
    exit(1);
}

unsigned long long get_process_starttime(pid_t pid) {
    char *proc_stat_path = NULL;
    if (asprintf(&proc_stat_path, "/proc/%ld/stat", (long) pid) == -1 || proc_stat_path == NULL) {
        return 0;
    }

    FILE *fp = fopen(proc_stat_path, "r");
    free(proc_stat_path);
    if (fp == NULL) {
        return 0;
    }

    char buffer[2048];
    if (fgets(buffer, sizeof(buffer), fp) == NULL) {
        fclose(fp);
        return 0;
    }
    fclose(fp);

    // Find last bracket `)` of field 2 for `executable` in the format `(executable)`.
    char *lastBracketIndex = strrchr(buffer, ')');
    if (lastBracketIndex == NULL) return 0;

    // Parse fields starting right after the last `)`.
    // Field 3 `state` begins immediately after `) `.
    // Field 22 is `starttime` (the 20th field after field 2).
    char state;
    int ppid, pgrp, session, tty_nr, tpgid;
    unsigned flags;
    unsigned long minflt, cminflt, majflt, cmajflt, utime, stime;
    long cutime, cstime, priority, nice, num_threads, itrealvalue;
    unsigned long long starttime;

    int parsed = sscanf(lastBracketIndex + 2,
        "%c %d %d %d %d %d %u %lu %lu %lu %lu %lu %lu %ld %ld %ld %ld %ld %ld %llu",
        &state, &ppid, &pgrp, &session, &tty_nr, &tpgid,
        &flags,
        &minflt, &cminflt, &majflt, &cmajflt, &utime, &stime,
        &cutime, &cstime, &priority, &nice, &num_threads, &itrealvalue,
        &starttime);

    return (parsed == 20) ? starttime : 0;
}

void generate_uuid(char* str) {
    sprintf(str, "%x%x-%x-%x-%x-%x%x%x",
            /* 64-bit Hex number */
            arc4random(), arc4random(),
            /* 32-bit Hex number */
            (uint32_t) getpid(),
            /* 32-bit Hex number of the form 4xxx (4 is the UUID version) */
            ((arc4random() & 0x0fff) | 0x4000),
            /* 32-bit Hex number in the range [0x8000, 0xbfff] */
            arc4random() % 0x3fff + 0x8000,
            /*  96-bit Hex number */
            arc4random(), arc4random(), arc4random());
}

// Thread function which reads from stdin and writes to socket.
void* transmit_stdin_to_socket(void* arg) {
    int output_server_socket = *((int*) arg);
    struct sockaddr_un remote_addr;
    socklen_t addrlen = sizeof(remote_addr);
    int output_client_socket = accept(output_server_socket,
                                      (struct sockaddr*) &remote_addr,
                                      &addrlen);
    if (output_client_socket == -1) {
        return NULL;
    }

    ssize_t len;
    char buffer[1024];
    while ((len = read(STDIN_FILENO, &buffer, sizeof(buffer))) > 0) {
        if (write(output_client_socket, buffer, (size_t) len) < 0) break;
    }
    // Close output socket on end of input:
    close(output_client_socket);
    return NULL;
}

// Main thread function which reads from input socket and writes to stdout.
int transmit_socket_to_stdout(int input_socket_fd) {
    ssize_t len;
    char buffer[1024];
    char cbuf[256];
    struct iovec io = { .iov_base = buffer, .iov_len = sizeof(buffer) };
    struct msghdr msg = { 0 };
    int fd = -1;  // An optional file descriptor received through the socket
    msg.msg_iov = &io;
    msg.msg_iovlen = 1;
    msg.msg_control = cbuf;
    msg.msg_controllen = sizeof(cbuf);
    while ((len = recvmsg(input_socket_fd, &msg, 0)) > 0) {
        struct cmsghdr * cmsg = CMSG_FIRSTHDR(&msg);
        if (cmsg && cmsg->cmsg_len == CMSG_LEN(sizeof(int))) {
            if (cmsg->cmsg_type == SCM_RIGHTS) {
                fd = *((int *) CMSG_DATA(cmsg));
            }
        }
        // A file descriptor must be accompanied by a non-empty message,
        // so we use "@" when we don't want any output.
        if (fd != -1 && len == 1 && buffer[0] == '@') { len = 0; }
        write(STDOUT_FILENO, buffer, len);
        msg.msg_controllen = sizeof(cbuf);
    }
    if (len < 0) perror("recvmsg()");
    return fd;
}

int run_api_command(int argc, char **argv) {
    // If only `--version` argument is passed
    if (argc == 2 && strcmp(argv[1], "--version") == 0) {
        fprintf(stdout, "%s\n", TERMUX_API_PACKAGE_VERSION);
        fflush(stdout);
        exit(0);
    }

    // Do not transform children into zombies when they terminate:
    struct sigaction sigchld_action = {
        .sa_handler = SIG_DFL,
        .sa_flags = SA_RESTART | SA_NOCLDSTOP | SA_NOCLDWAIT
    };
    sigaction(SIGCHLD, &sigchld_action, NULL);

    char input_addr_str[100];  // This program reads from it.
    char output_addr_str[100]; // This program writes to it.

    generate_uuid(input_addr_str);
    generate_uuid(output_addr_str);

    struct sockaddr_un input_addr = { .sun_family = AF_UNIX };
    struct sockaddr_un output_addr = { .sun_family = AF_UNIX };
    // Leave struct sockaddr_un.sun_path[0] as 0 and use the UUID
    // string as abstract linux namespace:
    strncpy(&input_addr.sun_path[1], input_addr_str, strlen(input_addr_str));
    strncpy(&output_addr.sun_path[1], output_addr_str, strlen(output_addr_str));

    int input_server_socket = socket(AF_UNIX, SOCK_STREAM|SOCK_CLOEXEC, 0);
    if (input_server_socket == -1) {
        perror("socket()");
        return -1;
    }
    int output_server_socket = socket(AF_UNIX, SOCK_STREAM|SOCK_CLOEXEC, 0);
    if (output_server_socket == -1) {
        perror("socket()");
        return -1;
    }

    int ret;
    ret = bind(input_server_socket, (struct sockaddr*) &input_addr,
               sizeof(sa_family_t) + strlen(input_addr_str) + 1);
    if (ret == -1) {
        perror("bind(input)");
        return ret;
    }

    ret = bind(output_server_socket, (struct sockaddr*) &output_addr,
               sizeof(sa_family_t) + strlen(output_addr_str) + 1);
    if (ret == -1) {
        perror("bind(output)");
        return ret;
    }

    if (listen(input_server_socket, 1) == -1) {
        perror("listen()");
        return -1;
    }

    if (listen(output_server_socket, 1) == -1) {
        perror("listen()");
        return -1;
    }

    // A `pid` and `starttime` combo can uniquely identify a process even if pid gets recycled.
    pid_t api_server_pid = getpid();
    uid_t api_server_uid = getuid();
    unsigned long long api_server_starttime = get_process_starttime(api_server_pid);

    pid_t fork_result = fork();
    if (fork_result == -1) {
        perror("fork()");
        return -1;
    } else if (fork_result == 0) {
        contact_plugin_v2(argc, argv,
            api_server_pid, api_server_uid, api_server_starttime,
            input_addr_str, output_addr_str);
    }

    struct sockaddr_un remote_addr;
    socklen_t addrlen = sizeof(remote_addr);
    int input_client_socket = accept(input_server_socket,
                                     (struct sockaddr*) &remote_addr,
                                     &addrlen);

    pthread_t transmit_thread;
    pthread_create(&transmit_thread, NULL, transmit_stdin_to_socket,
                   &output_server_socket);

    /* Device has been opened, time to actually get the fd */
    int fd = transmit_socket_to_stdout(input_client_socket);
    close(input_client_socket);
    return fd;
}
