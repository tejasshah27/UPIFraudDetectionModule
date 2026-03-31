package com.antigravity.fraud.controller;

import com.antigravity.fraud.model.UserProfile;
import com.antigravity.fraud.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> getUserProfile(
            @PathVariable String userId) {
        UserProfile profile = userProfileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
