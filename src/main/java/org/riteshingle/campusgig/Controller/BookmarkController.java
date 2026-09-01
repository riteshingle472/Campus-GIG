package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.ResponseDTO.BookmarkResponseDTO;
import org.riteshingle.campusgig.Service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/bookmark")
@RestController
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @PreAuthorize("hasRole('GIG')")
    @PostMapping("/bookmark")
    public ResponseEntity<?> saveJob(@RequestParam Long jobId){
        bookmarkService.bookmarkJob(jobId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('GIG')")
    @DeleteMapping("/bookmark")
    public ResponseEntity<?> deleteJob(@RequestParam Long jobId){
        bookmarkService.removeBookmarkJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('GIG')")
    @GetMapping("/bookmarks")
    public ResponseEntity<List<BookmarkResponseDTO>> getSaveJobs(){
        return ResponseEntity.ok(bookmarkService.bookmarkJobs());
    }
}
