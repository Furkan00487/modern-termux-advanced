#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

/**
 * Pure safety predicates and fallback pointer helpers for Termux:X11 DRI3/Present buffers.
 *
 * Numeric constants kept in sync with buffer.h and Android NDK hardware_buffer.h:
 * - LORIEBUFFER_FD == 2, LORIEBUFFER_AHARDWAREBUFFER == 3
 * - AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM == 1
 */
static inline bool lorieCanFlipBuffer(bool hasPriv,
                                      bool hasBuffer,
                                      bool hasMem,
                                      bool gpuOnly,
                                      bool sizeMatchesRoot,
                                      uint8_t type,
                                      uint8_t format,
                                      bool imported,
                                      bool forceFlip) {
    if (!hasPriv || !hasBuffer || hasMem || gpuOnly || !sizeMatchesRoot)
        return false;
    if (format == 1 /* AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM */)
        return false;
    if (type == 2 /* LORIEBUFFER_FD */ && imported && !forceFlip)
        return false;
    if (type != 2 && type != 3 /* LORIEBUFFER_AHARDWAREBUFFER */)
        return false;
    return true;
}

static inline void *lorieResolveCpuAccessPointer(void *lockedPtr,
                                                 void *memPtr,
                                                 void **fallbackPtr,
                                                 uint32_t stridePixels,
                                                 uint32_t height,
                                                 void *(*allocFn)(size_t, size_t)) {
    if (lockedPtr)
        return lockedPtr;
    if (memPtr)
        return memPtr;
    if (!fallbackPtr)
        return NULL;
    if (*fallbackPtr)
        return *fallbackPtr;
    if (!allocFn || stridePixels == 0 || height == 0)
        return NULL;

    size_t rowBytes = (size_t) stridePixels * 4u;
    if (rowBytes / 4u != stridePixels || (size_t) height > SIZE_MAX / rowBytes)
        return NULL;

    *fallbackPtr = allocFn((size_t) height, rowBytes);
    return *fallbackPtr;
}
