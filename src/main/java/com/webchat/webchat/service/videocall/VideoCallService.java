package com.webchat.webchat.service.videocall;

import com.webchat.webchat.dto.VideoCallDTO;
import com.webchat.webchat.dto.VideoCallSignalDTO;
import com.webchat.webchat.entity.VideoCall;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.VideoCallRepository;
import com.webchat.webchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoCallService {
    
    private final VideoCallRepository videoCallRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public VideoCall initiateCall(UUID callerId, UUID calleeId) {
        User caller = userRepository.findById(callerId).orElseThrow();
        User callee = userRepository.findById(calleeId).orElseThrow();
        
        // Check if either user is already in an active call
        Optional<VideoCall> callerActiveCall = videoCallRepository.findActiveCallByUser(caller);
        Optional<VideoCall> calleeActiveCall = videoCallRepository.findActiveCallByUser(callee);
        
        if (callerActiveCall.isPresent() || calleeActiveCall.isPresent()) {
            throw new RuntimeException("User is already in an active call");
        }
        
        VideoCall videoCall = new VideoCall();
        videoCall.setCaller(caller);
        videoCall.setCallee(callee);
        videoCall.setStatus(VideoCall.CallStatus.INITIATED);
        VideoCall saved = videoCallRepository.save(videoCall);
        
        // Notify callee about incoming call
        VideoCallDTO callDto = convertToDTO(saved);
        System.out.println("📞 Sending video call notification to: /topic/video-call/" + calleeId);
        System.out.println("📞 Call DTO: " + callDto);
        messagingTemplate.convertAndSend("/topic/video-call/" + calleeId, callDto);
        
        return saved;
    }
    
    public void handleCallSignal(VideoCallSignalDTO signal) {
        VideoCall call = videoCallRepository.findById(signal.getCallId()).orElseThrow();
        
        switch (signal.getType()) {
            case CALL_OFFER:
                call.setStatus(VideoCall.CallStatus.RINGING);
                videoCallRepository.save(call);
                // Forward offer to callee
                messagingTemplate.convertAndSend("/topic/video-signal/" + signal.getToUserId(), signal);
                break;
                
            case CALL_ANSWER:
                // Forward answer to caller
                messagingTemplate.convertAndSend("/topic/video-signal/" + signal.getToUserId(), signal);
                break;
                
            case ICE_CANDIDATE:
                // Forward ICE candidate to the other peer
                messagingTemplate.convertAndSend("/topic/video-signal/" + signal.getToUserId(), signal);
                break;
                
            case CALL_ACCEPT:
                call.setStatus(VideoCall.CallStatus.ACCEPTED);
                call.setStartedAt(LocalDateTime.now());
                videoCallRepository.save(call);
                // Notify caller that call was accepted
                messagingTemplate.convertAndSend("/topic/video-signal/" + signal.getToUserId(), signal);
                break;
                
            case CALL_REJECT:
                call.setStatus(VideoCall.CallStatus.REJECTED);
                call.setEndedAt(LocalDateTime.now());
                videoCallRepository.save(call);
                // Notify caller that call was rejected
                messagingTemplate.convertAndSend("/topic/video-signal/" + signal.getToUserId(), signal);
                break;
                
            case CALL_END:
                endCall(call);
                // Notify the other party that call ended
                messagingTemplate.convertAndSend("/topic/video-signal/" + signal.getToUserId(), signal);
                break;
        }
    }
    
    public void endCall(VideoCall call) {
        if (call.getStatus() == VideoCall.CallStatus.ACCEPTED && call.getStartedAt() != null) {
            LocalDateTime endTime = LocalDateTime.now();
            call.setEndedAt(endTime);
            call.setDurationSeconds(ChronoUnit.SECONDS.between(call.getStartedAt(), endTime));
        }
        call.setStatus(VideoCall.CallStatus.ENDED);
        videoCallRepository.save(call);
    }
    
    public void endCallById(UUID callId) {
        VideoCall call = videoCallRepository.findById(callId).orElseThrow();
        endCall(call);
        
        // Notify both parties
        VideoCallSignalDTO endSignal = new VideoCallSignalDTO();
        endSignal.setCallId(callId);
        endSignal.setType(VideoCallSignalDTO.SignalType.CALL_END);
        
        messagingTemplate.convertAndSend("/topic/video-signal/" + call.getCaller().getIdUser(), endSignal);
        messagingTemplate.convertAndSend("/topic/video-signal/" + call.getCallee().getIdUser(), endSignal);
    }
    
    public List<VideoCallDTO> getCallHistory(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return videoCallRepository.findCallHistoryByUser(user)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    public Optional<VideoCallDTO> getActiveCall(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return videoCallRepository.findActiveCallByUser(user)
                .map(this::convertToDTO);
    }
    
    public void acceptCall(UUID callId) {
        VideoCall call = videoCallRepository.findById(callId).orElseThrow();
        call.setStatus(VideoCall.CallStatus.ACCEPTED);
        call.setStartedAt(LocalDateTime.now());
        videoCallRepository.save(call);
        
        // Notify both parties
        VideoCallDTO callDTO = convertToDTO(call);
        messagingTemplate.convertAndSend("/topic/video-call/" + call.getCaller().getIdUser(), callDTO);
        messagingTemplate.convertAndSend("/topic/video-call/" + call.getCallee().getIdUser(), callDTO);
    }
    
    public void rejectCall(UUID callId) {
        VideoCall call = videoCallRepository.findById(callId).orElseThrow();
        call.setStatus(VideoCall.CallStatus.REJECTED);
        call.setEndedAt(LocalDateTime.now());
        videoCallRepository.save(call);
        
        // Notify both parties
        VideoCallDTO callDTO = convertToDTO(call);
        messagingTemplate.convertAndSend("/topic/video-call/" + call.getCaller().getIdUser(), callDTO);
        messagingTemplate.convertAndSend("/topic/video-call/" + call.getCallee().getIdUser(), callDTO);
    }
    
    private VideoCallDTO convertToDTO(VideoCall videoCall) {
        return new VideoCallDTO(
                videoCall.getId(),
                videoCall.getCaller().getIdUser(),
                videoCall.getCaller().getFirstName() + " " + videoCall.getCaller().getLastName(),
                videoCall.getCaller().getAvatar(),
                videoCall.getCallee().getIdUser(),
                videoCall.getCallee().getFirstName() + " " + videoCall.getCallee().getLastName(),
                videoCall.getCallee().getAvatar(),
                videoCall.getStatus().name(),
                videoCall.getCreatedAt(),
                videoCall.getStartedAt(),
                videoCall.getEndedAt(),
                videoCall.getDurationSeconds()
        );
    }
}