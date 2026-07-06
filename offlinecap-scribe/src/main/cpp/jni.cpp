#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <mutex>
#include <thread>
#include <chrono>
#include <android/log.h>
#include "whisper.h"

#define LOG_TAG "OfflineCapScribe"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::map<int, bool> abort_flags;
static std::mutex abort_mutex;

static std::map<int, int> progress_percent;
static std::mutex progress_mutex;

struct JobContext {
    int job_id;
};

static int resolve_thread_count() {
    unsigned int hw = std::thread::hardware_concurrency();
    if (hw == 0) hw = 4;
    return std::max(2, std::min(8, static_cast<int>(hw)));
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_io_github_alirezajavan_offlinecap_scribe_WhisperJni_initContext(
        JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    struct whisper_context_params params = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(model_path, path);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_io_github_alirezajavan_offlinecap_scribe_WhisperJni_freeContext(
        JNIEnv *env, jobject thiz, jlong ptr) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(ptr);
    if (ctx) {
        whisper_free(ctx);
    }
}

JNIEXPORT void JNICALL
Java_io_github_alirezajavan_offlinecap_scribe_WhisperJni_cancel(
        JNIEnv *env, jobject thiz, jint job_id) {
    std::lock_guard<std::mutex> lock(abort_mutex);
    abort_flags[job_id] = true;
}

JNIEXPORT jint JNICALL
Java_io_github_alirezajavan_offlinecap_scribe_WhisperJni_getProgress(
        JNIEnv *env, jobject thiz, jint job_id) {
    std::lock_guard<std::mutex> lock(progress_mutex);
    auto it = progress_percent.find(job_id);
    return it != progress_percent.end() ? it->second : 0;
}

JNIEXPORT jobjectArray JNICALL
Java_io_github_alirezajavan_offlinecap_scribe_WhisperJni_transcribeWindow(
        JNIEnv *env, jobject thiz, jlong ptr, jfloatArray samples, jlong offset_ms, jstring lang, jint job_id,
        jint best_of, jboolean temperature_fallback) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(ptr);
    if (!ctx) return nullptr;

    jfloat *samples_ptr = env->GetFloatArrayElements(samples, nullptr);
    jsize samples_len = env->GetArrayLength(samples);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    const char *lang_str = nullptr;
    if (lang != nullptr) {
        lang_str = env->GetStringUTFChars(lang, nullptr);
        params.language = lang_str;
    }

    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.n_threads = resolve_thread_count();
    params.greedy.best_of = best_of;
    if (!temperature_fallback) {
        // A zero increment disables whisper.cpp's retry-at-higher-temperature
        // passes, which can multiply the cost of a difficult window.
        params.temperature_inc = 0.0f;
    }

    JobContext job_ctx { job_id };

    params.abort_callback = [](void * user_data) {
        int id = reinterpret_cast<JobContext*>(user_data)->job_id;
        std::lock_guard<std::mutex> lock(abort_mutex);
        return (bool)(abort_flags.count(id) > 0 && abort_flags[id]);
    };
    params.abort_callback_user_data = &job_ctx;

    params.progress_callback = [](struct whisper_context * /*ctx*/, struct whisper_state * /*state*/,
                                   int progress, void * user_data) {
        int id = reinterpret_cast<JobContext*>(user_data)->job_id;
        std::lock_guard<std::mutex> lock(progress_mutex);
        progress_percent[id] = progress;
    };
    params.progress_callback_user_data = &job_ctx;

    {
        std::lock_guard<std::mutex> lock(abort_mutex);
        abort_flags.erase(job_id);
    }
    {
        std::lock_guard<std::mutex> lock(progress_mutex);
        progress_percent[job_id] = 0;
    }

    LOGI("transcribeWindow start job=%d samples=%d threads=%d", job_id, samples_len, params.n_threads);
    auto start_time = std::chrono::steady_clock::now();

    int whisper_result = whisper_full(ctx, params, samples_ptr, samples_len);

    auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - start_time).count();
    LOGI("transcribeWindow end job=%d elapsed_ms=%lld", job_id, static_cast<long long>(elapsed_ms));

    if (whisper_result != 0) {
        env->ReleaseFloatArrayElements(samples, samples_ptr, JNI_ABORT);
        if (lang_str != nullptr) {
            env->ReleaseStringUTFChars(lang, lang_str);
        }
        LOGE("whisper_full failed job=%d rc=%d", job_id, whisper_result);
        return nullptr;
    }

    {
        std::lock_guard<std::mutex> lock(progress_mutex);
        progress_percent[job_id] = 100;
    }

    int n_segments = whisper_full_n_segments(ctx);
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(n_segments * 3, string_class, nullptr);

    for (int i = 0; i < n_segments; ++i) {
        long long t0 = whisper_full_get_segment_t0(ctx, i);
        long long t1 = whisper_full_get_segment_t1(ctx, i);
        const char *text = whisper_full_get_segment_text(ctx, i);

        long long start_ms = offset_ms + t0 * 10;
        long long end_ms = offset_ms + t1 * 10;

        env->SetObjectArrayElement(result, i * 3, env->NewStringUTF(std::to_string(start_ms).c_str()));
        env->SetObjectArrayElement(result, i * 3 + 1, env->NewStringUTF(std::to_string(end_ms).c_str()));
        env->SetObjectArrayElement(result, i * 3 + 2, env->NewStringUTF(text));
    }

    env->ReleaseFloatArrayElements(samples, samples_ptr, JNI_ABORT);
    if (lang_str != nullptr) {
        env->ReleaseStringUTFChars(lang, lang_str);
    }

    return result;
}

JNIEXPORT jstring JNICALL
Java_io_github_alirezajavan_offlinecap_scribe_WhisperJni_detectLanguage(
        JNIEnv *env, jobject thiz, jlong ptr, jfloatArray samples) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(ptr);
    if (!ctx) return nullptr;

    jfloat *samples_ptr = env->GetFloatArrayElements(samples, nullptr);
    jsize samples_len = env->GetArrayLength(samples);

    if (whisper_pcm_to_mel(ctx, samples_ptr, samples_len, 1) != 0) {
        env->ReleaseFloatArrayElements(samples, samples_ptr, JNI_ABORT);
        return nullptr;
    }

    std::vector<float> probs(whisper_lang_max_id() + 1, 0.0f);
    int lang_id = whisper_lang_auto_detect(ctx, 0, 1, probs.data());

    env->ReleaseFloatArrayElements(samples, samples_ptr, JNI_ABORT);

    if (lang_id >= 0) {
        return env->NewStringUTF(whisper_lang_str(lang_id));
    }
    return nullptr;
}

}
