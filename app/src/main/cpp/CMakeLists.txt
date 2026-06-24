cmake_minimum_required(VERSION 3.22.1)

project("myplayer_security")

# ── Security compiler flags ───────────────────────────────────────────────────
# -fstack-protector-strong : Stack canaries on functions with arrays/alloca
# -D_FORTIFY_SOURCE=2       : Runtime buffer overflow detection
# -O2                       : Optimise — harder to understand in disassembly
# -fvisibility=hidden       : Only JNI-exported functions are visible; others
#                             are hidden from symbol tables (harder to hook)
# -fPIC                     : Position Independent Code (required for .so)
# -Wl,--exclude-libs,ALL   : Don't export symbols from static libs
set(SECURITY_FLAGS
    "-fstack-protector-strong"
    "-D_FORTIFY_SOURCE=2"
    "-O2"
    "-fvisibility=hidden"
    "-fPIC"
)

add_library(
    myplayer_security        # Library name (must match System.loadLibrary() call)
    SHARED                   # Shared (.so) — loaded at runtime
    native-lib.cpp           # Source file
)

target_compile_options(myplayer_security PRIVATE ${SECURITY_FLAGS})

target_link_options(myplayer_security PRIVATE
    "-Wl,--exclude-libs,ALL"  # Hide all symbols from linked static libs
    "-Wl,-z,noexecstack"      # Non-executable stack
    "-Wl,-z,relro"            # Read-only relocations after linking
    "-Wl,-z,now"              # Resolve all symbols at load time (prevents lazy binding attacks)
)

find_library(log-lib log)    # Android logging
target_link_libraries(myplayer_security ${log-lib})
