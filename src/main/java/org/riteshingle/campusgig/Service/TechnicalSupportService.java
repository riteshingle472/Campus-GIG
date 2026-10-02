package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.TechnicalSupportStatus;
import org.riteshingle.campusgig.Exception.EmailSendingException;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Model.TechnicalSupport;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.TechnicalSupportRepository;
import org.riteshingle.campusgig.RequestDTO.TechnicalSupportRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.TechnicalSupportResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TechnicalSupportService {
    private final EmailService emailService;
    private final AuthService authService;
    private final TechnicalSupportRepository technicalSupportRepository;

//    Post Technical Issue
    public void postTechnicalIssue(TechnicalSupportRequestDTO dto){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Create Technical Support Entity
        TechnicalSupport technicalSupport = toEntity(dto);
//        Set Reporting in entity
        technicalSupport.setReporter(currentProfile);

        String subject = "Technical Issue Received – Campus GIG Support";
        String body = "Hi "+currentProfile.getFirstName()+" "+currentProfile.getLastName()+",\n" +
                "\n" +
                "Thank you for contacting Campus GIG Support.\n" +
                "\n" +
                "We have successfully received your "+dto.getSubject()+" technical issue. Our Campus GIG support team will review the issue and work to resolve it within 24 to 48 hours.\n" +
                "\n" +
                "If we need any additional information from you, our team will contact you.\n" +
                "\n" +
                "Thank you for your patience and for using Campus GIG.\n" +
                "\n" +
                "Best Regards,\n" +
                "Campus GIG Team";

        try {
            emailService.sendMail(currentProfile.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send technical support confirmation email"+ e);
        }

        technicalSupportRepository.save(technicalSupport);
    }

//    Get Technical Issue by ID
    public TechnicalSupportResponseDTO getMyTechnicalIssueById(Long id){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Find Technical Support by ID
        TechnicalSupport technicalSupport = technicalSupportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technical Issue not found by ID : " + id));

        if(!technicalSupport.getReporter().getId().equals(currentProfile.getId())){
            throw new ForbiddenException("You aren't authorized to view others Technical Issues");
        }

//        Convert into DTO and return Response
        return toResponse(technicalSupport);
    }

//    Technical Supports
    public List<TechnicalSupportResponseDTO> getMyTechnicalReports(Pageable pageable){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Fetch Technical Supports (Issue) by current Logged-in user
        List<TechnicalSupport> technicalSupportList = technicalSupportRepository.findMyTechnicalSupportReport(currentProfile,pageable);
//        Convert in DTOs List and return List Response
        return technicalSupportList.stream().map(this::toResponse).toList();
    }

//    Helper methods
    private TechnicalSupport toEntity(TechnicalSupportRequestDTO dto){
        return TechnicalSupport.builder()
                .status(TechnicalSupportStatus.PENDING)
                .description(dto.getDescription())
                .subject(dto.getSubject())
                .build();
    }

    private TechnicalSupportResponseDTO toResponse(TechnicalSupport technicalSupport){
        return TechnicalSupportResponseDTO.builder()
                .id(technicalSupport.getId())
                .status(technicalSupport.getStatus())
                .subject(technicalSupport.getSubject())
                .description(technicalSupport.getDescription())
                .createdAt(technicalSupport.getCreatedAt())
                .build();
    }
}
