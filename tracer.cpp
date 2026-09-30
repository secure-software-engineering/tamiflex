/*******************************************************************************
 * Copyright (c) 2026 Gauravsingh Sisodia.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gauravsingh Sisodia - initial implementation
 ******************************************************************************/

/*
 * JVMTI agent for generating dynamic call graphs
 *
 * Build:
 *   g++ -O3 -fPIC -shared -o tracer.so \
 *       -I/usr/lib/jvm/java-openjdk/include \
 *       -I/usr/lib/jvm/java-openjdk/include/linux \
 *       tracer.cpp
 *
 * Usage:
 *   java -agentpath:./tracer.so=dynamic_call_graph.log \
 *        -jar dacapo-23.11-MR2-chopin.jar <benchmark> -s small
 *
 * The agent records method invocation events and writes them to the
 * specified log file. The resulting trace can be converted to GXL
 * using Trace2GXL.java from the TamiFlex benchmarks repository:
 *
 *   https://github.com/secure-software-engineering/tamiflex.benchmarks/tree/master/jvmti-tracer
 */

#include <iostream>
#include <fstream>
#include <string>
#include <unordered_set>
#include <mutex>
#include <jvmti.h>

static std::ofstream out_file;

// For unique edges
static std::unordered_set<std::string> seen_edges;
static std::mutex edge_mutex;

static void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError error, const char *msg) {
    if (error != JVMTI_ERROR_NONE) {
        char *name = nullptr;
        jvmti->GetErrorName(error, &name);
        std::cerr << "ERROR: JVMTI: " << error << "(" << (name == nullptr ? "Unknown" : name) << "): " << (msg == nullptr ? "" : msg) << std::endl;
        if (name != nullptr) {
            jvmti->Deallocate((unsigned char*)name);
        }
    }
}

static const int CALL_CHAIN_LENGTH = 2;

void JNICALL method_entry_callback(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread, jmethodID mid) {
    jvmtiError error;
    int depth;
    
    char *method_names[CALL_CHAIN_LENGTH] = {nullptr, nullptr};
    char *method_signatures[CALL_CHAIN_LENGTH] = {nullptr, nullptr};
    char *class_signatures[CALL_CHAIN_LENGTH] = {nullptr, nullptr};
    int frames_found = 0;

    for (depth = 0; depth < CALL_CHAIN_LENGTH; depth++) {
        jmethodID method;
        jlocation location;
        jclass declaring_class;

        if (jvmti->GetFrameLocation(thread, depth, &method, &location) == JVMTI_ERROR_NO_MORE_FRAMES) {
            break;
        }

        jvmti->GetMethodDeclaringClass(method, &declaring_class);
        jvmti->GetClassSignature(declaring_class, &class_signatures[depth], nullptr);
        jni->DeleteLocalRef(declaring_class);

        jvmti->GetMethodName(method, &method_names[depth], &method_signatures[depth], nullptr);
        frames_found++;
    }

    if (frames_found > 0) {
        std::string edge_str = "CALL\t";
        for (depth = 0; depth < CALL_CHAIN_LENGTH; depth++) {
            if (depth < frames_found) {
                edge_str += class_signatures[depth];
                edge_str += '\t';
                edge_str += method_names[depth];
                edge_str += '\t';
                edge_str += method_signatures[depth];
            } else {
                edge_str += "ROOT";
            }

            if (depth != CALL_CHAIN_LENGTH - 1) {
                edge_str += '\t';
            }
        }

        {
            std::lock_guard<std::mutex> lock(edge_mutex);
            if (seen_edges.insert(edge_str).second) {
                out_file << edge_str << "\n";
            }
        }
    }

    for (depth = 0; depth < frames_found; depth++) {
        jvmti->Deallocate((unsigned char*) method_names[depth]);
        jvmti->Deallocate((unsigned char*) method_signatures[depth]);
        jvmti->Deallocate((unsigned char*) class_signatures[depth]);
    }
}

void JNICALL vm_death_callback(jvmtiEnv *jvmti, JNIEnv* jni) {
    // Wait briefly to allow threads to finish writing their final edges
}

static const jvmtiEventCallbacks callbacks = {
    .VMDeath = &vm_death_callback,
    .MethodEntry = &method_entry_callback,
};

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv *jvmti = nullptr;
    jvmtiCapabilities capabilities;
    jvmtiError error;

    if (options == nullptr) {
        std::cerr << "This agent requires the following option: <file>." << std::endl;
        return JNI_ERR;
    }

    out_file.open(options, std::ios::out | std::ios::trunc);
    if (!out_file.is_open()) {
        std::cerr << "Couldn't open file " << options << " for writing" << std::endl;
        return JNI_ERR;
    }

    if (jvm->GetEnv((void **) &jvmti, JVMTI_VERSION) != JNI_OK) {
        std::cerr << "Couldn't get JVMTI environment" << std::endl;
        return JNI_ERR;
    }

    error = jvmti->GetCapabilities(&capabilities);
    check_jvmti_error(jvmti, error, "Couldn't get capabilities");

    capabilities.can_generate_method_entry_events = 1;

    error = jvmti->AddCapabilities(&capabilities);
    check_jvmti_error(jvmti, error, "Couldn't add capabilities");

    jvmtiEventCallbacks cb = {0};
    cb.MethodEntry = &method_entry_callback;
    cb.VMDeath = &vm_death_callback;

    error = jvmti->SetEventCallbacks(&cb, sizeof(cb));
    check_jvmti_error(jvmti, error, "Couldn't set event callbacks");

    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, nullptr);
    check_jvmti_error(jvmti, error, "Couldn't enable notification on method entry");

    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, nullptr);
    check_jvmti_error(jvmti, error, "Couldn't enable notification on VM death");

    return JNI_OK;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    if (out_file.is_open()) {
        out_file.flush();
        out_file.close();
    }
}
