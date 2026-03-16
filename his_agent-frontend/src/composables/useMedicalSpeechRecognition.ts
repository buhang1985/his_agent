import { ref, onUnmounted } from 'vue';
import { IFlytekService } from '@/services/speech/IFlytekService';
import type { ConversationSegment } from '@/types/voice';

interface UseSpeechRecognitionOptions {
  appId: string;
  apiKey: string;
  domain?: 'medical' | 'general';
  onTranscriptUpdate?: (text: string, isFinal: boolean) => void;
}

export function useMedicalSpeechRecognition(options: UseSpeechRecognitionOptions) {
  const { appId, apiKey, domain, onTranscriptUpdate } = options;

  const service = new IFlytekService({ appId, apiKey, domain });

  const transcript = ref('');
  const segments = ref<ConversationSegment[]>([]);
  const status = ref<'idle' | 'recording' | 'processing' | 'stopped'>('idle');

  service.onResult = (result) => {
    if (result.isFinal) {
      segments.value.push({
        id: `seg_${Date.now()}`,
        speaker: 'doctor',
        text: result.text,
        timestamp: result.startTime,
        confidence: result.confidence,
      });
      transcript.value += result.text;
      onTranscriptUpdate?.(result.text, true);
    } else {
      onTranscriptUpdate?.(result.text, false);
    }
  };

  service.onStatusChange = (newStatus) => {
    status.value = newStatus as typeof status.value;
  };

  service.onError = (error) => {
    console.error('语音识别错误:', error);
  };

  const start = async () => {
    transcript.value = '';
    segments.value = [];
    await service.start();
  };

  const stop = async () => {
    await service.stop();
  };

  onUnmounted(() => {
    stop();
  });

  return {
    transcript,
    segments,
    status,
    error: service.error,
    sid: service.sid,
    start,
    stop,
  };
}
