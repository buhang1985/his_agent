import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface SpeechState {
  isRecording: boolean;
  isTranscribing: boolean;
  audioBlob: Blob | null;
  transcription: string | null;
  error: string | null;
}

/**
 * 语音 Store
 * 管理录音状态、转写进度、音频数据等
 */
export const useSpeechStore = defineStore('speech', () => {
  // State
  const isRecording = ref(false);
  const isTranscribing = ref(false);
  const audioBlob = ref<Blob | null>(null);
  const transcription = ref<string | null>(null);
  const error = ref<string | null>(null);
  const recordingTime = ref(0);

  // Actions
  function startRecording() {
    isRecording.value = true;
    recordingTime.value = 0;
    error.value = null;
  }

  function stopRecording(blob: Blob) {
    isRecording.value = false;
    audioBlob.value = blob;
  }

  function startTranscribing() {
    isTranscribing.value = true;
    error.value = null;
  }

  function setTranscription(text: string) {
    transcription.value = text;
    isTranscribing.value = false;
  }

  function setError(message: string) {
    error.value = message;
    isTranscribing.value = false;
    isRecording.value = false;
  }

  function reset() {
    isRecording.value = false;
    isTranscribing.value = false;
    audioBlob.value = null;
    transcription.value = null;
    error.value = null;
    recordingTime.value = 0;
  }

  function incrementRecordingTime() {
    recordingTime.value++;
  }

  return {
    // State
    isRecording,
    isTranscribing,
    audioBlob,
    transcription,
    error,
    recordingTime,
    // Actions
    startRecording,
    stopRecording,
    startTranscribing,
    setTranscription,
    setError,
    reset,
    incrementRecordingTime,
  };
});
