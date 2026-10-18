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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class TechnicalSupportService {
    private final EmailService emailService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TechnicalSupportRepository technicalSupportRepository;

//    Post Technical Issue
    @CachePut(value = "TechnicalSupport",key = "#result.id")
    public TechnicalSupportResponseDTO postTechnicalIssue(TechnicalSupportRequestDTO dto){
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
//            emailService.sendMail(currentProfile.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send technical support confirmation email"+ e);
        }

        technicalSupportRepository.save(technicalSupport);
        return toResponse(technicalSupport);
    }

//    Get Technical Issue by ID
    @Cacheable(value = "TechnicalSupport",key = "#id")
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
    public List<TechnicalSupportResponseDTO> getMyTechnicalReports(int page, int size,String field,String direction){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

        String key = "MyTechnicalSupport:page:"+page+":size:"+size+":direction:"+direction+":field:"+field;
        try {
            String cacheTechnicalSupports = stringRedisTemplate.opsForValue().get(key);
            if(cacheTechnicalSupports != null && !cacheTechnicalSupports.isBlank()){
                return objectMapper.convertValue(cacheTechnicalSupports, new TypeReference<>() {});
            }
        }catch (Exception e){
            stringRedisTemplate.delete(key);
        }

//        Creating Pageable Object & Fetch Technical Supports (Issue) of current Logged-in user
        Pageable pageable = PageRequest.of(page - 1 , size, Sort.by(Sort.Direction.fromString(direction),field));
        List<TechnicalSupport> technicalSupportList = technicalSupportRepository.findMyTechnicalSupportReport(currentProfile,pageable);
//        Convert in DTOs List and return List Response
        List<TechnicalSupportResponseDTO> technicalSupportResponseDTOsList = technicalSupportList.stream().map(this::toResponse).toList();
        stringRedisTemplate.opsForValue().set(key,objectMapper.writeValueAsString(technicalSupportResponseDTOsList),15, TimeUnit.MINUTES);

        return technicalSupportResponseDTOsList;
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
