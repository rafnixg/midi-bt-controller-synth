cmake_minimum_required(VERSION 3.22.1)
project(midi-bt-controller)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(FLUIDSYNTH_DIR ${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth)
set(LIBS_DIR ${FLUIDSYNTH_DIR}/lib/${ANDROID_ABI})

# Definimos las librerías necesarias
set(FS_LIBS 
    fluidsynth oboe glib-2.0 gthread-2.0 gmodule-2.0 gobject-2.0 gio-2.0 
    sndfile FLAC opus vorbis vorbisenc vorbisfile ogg pcre instpatch-1.0
)

# Importamos cada una
foreach(LIB ${FS_LIBS})
    add_library(${LIB} SHARED IMPORTED)
    set_target_properties(${LIB} PROPERTIES IMPORTED_LOCATION ${LIBS_DIR}/lib${LIB}.so)
endforeach()

# Nuestra librería
add_library(synth-lib SHARED SynthManager.cpp)

target_include_directories(synth-lib PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${FLUIDSYNTH_DIR}/include)
target_compile_definitions(synth-lib PRIVATE HAS_FLUIDSYNTH=1)

find_library(log-lib log)
find_library(android-lib android)

# Enlazamos TODO. Al enlazar aquí, Android intenta cargar las dependencias automáticamente.
target_link_libraries(synth-lib 
    ${log-lib} 
    ${android-lib} 
    ${FS_LIBS}
)
