cmake_minimum_required(VERSION 3.22.1)

project("offlinecap_scribe")

set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper)

# We assume whisper.cpp is vendored in 'whisper' directory
# and it has its own CMakeLists.txt
if (EXISTS ${WHISPER_DIR}/CMakeLists.txt)
    add_subdirectory(${WHISPER_DIR})
    set(WHISPER_LIB whisper)
else()
    message(WARNING "whisper.cpp source not found at ${WHISPER_DIR}")
    # Placeholder for build to not fail completely during configuration
    add_library(whisper_stub STATIC stub.cpp)
    set(WHISPER_LIB whisper_stub)
endif()

add_library(offlinecap_scribe SHARED
    jni.cpp
)

target_include_directories(offlinecap_scribe PRIVATE
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/ggml/include
)

target_link_libraries(offlinecap_scribe
    ${WHISPER_LIB}
    log
    android
)
