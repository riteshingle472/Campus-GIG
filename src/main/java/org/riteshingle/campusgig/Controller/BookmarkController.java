package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.ResponseDTO.BookmarkResponseDTO;
import org.riteshingle.campusgig.Service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/save-job")
@RestController
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @PreAuthorize("hasRole('GIG')")
    @PostMapping("/save")
    public ResponseEntity<?> saveJob(@RequestParam Long jobId){
        bookmarkService.bookmarkJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('GIG')")
    @DeleteMapping("/remove")
    public ResponseEntity<?> deleteJob(@RequestParam Long jobId){
        bookmarkService.removeBookmarkJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('GIG')")
    @GetMapping("/saves")
    public ResponseEntity<List<BookmarkResponseDTO>> getSaveJobs(){
        return ResponseEntity.ok(bookmarkService.bookmarkJobs());
    }
}
