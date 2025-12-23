package com.example.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_role", columnList = "role"),
    @Index(name = "idx_user_active", columnList = "is_active")
})
public class User {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verification_token_expires_at")
    private LocalDateTime emailVerificationTokenExpiresAt;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "facebook_id", unique = true)
    private String facebookId;

    @Column(name = "provider")
    private String provider; // GOOGLE, FACEBOOK, etc.

    @Column(name = "interests", columnDefinition = "LONGTEXT")
    private String interests;

    @Column(name = "preferences", columnDefinition = "LONGTEXT")
    private String preferences;

    @Column(name = "behaviors", columnDefinition = "LONGTEXT")
    private String behaviors;

    @Column(name = "personalization_enabled")
    private Boolean personalizationEnabled = true;

    @Column(name = "chatbot_enabled")
    private Boolean chatbotEnabled = true;

    @Column(name = "recommendation_enabled")
    private Boolean recommendationEnabled = true;

    @Column(name = "notification_settings", columnDefinition = "LONGTEXT")
    private String notificationSettings;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_favorite_products", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "product_id", length = 36)
    private Set<String> favoriteProductIds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public LocalDateTime getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDateTime dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public Boolean getIsEmailVerified() { return isEmailVerified; }
    public void setIsEmailVerified(Boolean isEmailVerified) { this.isEmailVerified = isEmailVerified; }
    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }
    public LocalDateTime getEmailVerificationTokenExpiresAt() { return emailVerificationTokenExpiresAt; }
    public void setEmailVerificationTokenExpiresAt(LocalDateTime emailVerificationTokenExpiresAt) { this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }
    public LocalDateTime getPasswordResetTokenExpiresAt() { return passwordResetTokenExpiresAt; }
    public void setPasswordResetTokenExpiresAt(LocalDateTime passwordResetTokenExpiresAt) { this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Integer getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(Integer loginAttempts) { this.loginAttempts = loginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getFacebookId() { return facebookId; }
    public void setFacebookId(String facebookId) { this.facebookId = facebookId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public String getBehaviors() { return behaviors; }
    public void setBehaviors(String behaviors) { this.behaviors = behaviors; }
    public Boolean getPersonalizationEnabled() { return personalizationEnabled; }
    public void setPersonalizationEnabled(Boolean personalizationEnabled) { this.personalizationEnabled = personalizationEnabled; }
    public Boolean getChatbotEnabled() { return chatbotEnabled; }
    public void setChatbotEnabled(Boolean chatbotEnabled) { this.chatbotEnabled = chatbotEnabled; }
    public Boolean getRecommendationEnabled() { return recommendationEnabled; }
    public void setRecommendationEnabled(Boolean recommendationEnabled) { this.recommendationEnabled = recommendationEnabled; }
    public String getNotificationSettings() { return notificationSettings; }
    public void setNotificationSettings(String notificationSettings) { this.notificationSettings = notificationSettings; }
    public Set<String> getFavoriteProductIds() { return favoriteProductIds; }
    public void setFavoriteProductIds(Set<String> favoriteProductIds) { this.favoriteProductIds = favoriteProductIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Check if email verification token is valid
     */
    public boolean isEmailVerificationTokenValid() {
        return emailVerificationTokenExpiresAt != null && 
               emailVerificationTokenExpiresAt.isAfter(LocalDateTime.now());
    }
}


