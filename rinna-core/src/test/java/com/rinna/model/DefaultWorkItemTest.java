package com.rinna.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultWorkItemTest {

    @Test
    void shouldCreateWorkItemWithRequiredFields() {
        // Given
        String title = "Test Item";
        WorkItemType type = WorkItemType.BUG;
        
        // When
        DefaultWorkItem item = DefaultWorkItem.builder()
                .title(title)
                .type(type)
                .build();
        
        // Then
        assertThat(item.getId()).isNotNull();
        assertThat(item.getTitle()).isEqualTo(title);
        assertThat(item.getType()).isEqualTo(type);
        assertThat(item.getStatus()).isEqualTo(WorkflowState.TO_DO);
        assertThat(item.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(item.getDescription()).isEmpty();
        assertThat(item.getAssignee()).isNull();
        assertThat(item.getParentId()).isEmpty();
        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldRequireTitle() {
        // Given
        DefaultWorkItem.Builder builder = DefaultWorkItem.builder()
                .type(WorkItemType.BUG);
        
        // Then
        assertThrows(IllegalStateException.class, builder::build);
    }
    
    @Test
    void shouldRequireType() {
        // Given
        DefaultWorkItem.Builder builder = DefaultWorkItem.builder()
                .title("Test Item");
        
        // Then
        assertThrows(IllegalStateException.class, builder::build);
    }
    
    @Test
    void shouldCreateFromRequest() {
        // Given
        String title = "Test Item";
        String description = "This is a test item";
        WorkItemType type = WorkItemType.FEATURE;
        Priority priority = Priority.HIGH;
        String assignee = "user1";
        UUID parentId = UUID.randomUUID();
        
        WorkItemCreateRequest request = WorkItemCreateRequest.builder()
                .title(title)
                .description(description)
                .type(type)
                .priority(priority)
                .assignee(assignee)
                .parentId(parentId)
                .build();
        
        // When
        DefaultWorkItem item = DefaultWorkItem.fromCreateRequest(request);
        
        // Then
        assertThat(item.getTitle()).isEqualTo(title);
        assertThat(item.getDescription()).isEqualTo(description);
        assertThat(item.getType()).isEqualTo(type);
        assertThat(item.getPriority()).isEqualTo(priority);
        assertThat(item.getAssignee()).isEqualTo(assignee);
        assertThat(item.getParentId()).isPresent();
        assertThat(item.getParentId().get()).isEqualTo(parentId);
    }
    
    @Test
    void shouldCreateWithNewStatus() {
        // Given
        DefaultWorkItem item = DefaultWorkItem.builder()
                .title("Test Item")
                .type(WorkItemType.BUG)
                .build();
        WorkflowState newStatus = WorkflowState.IN_PROGRESS;
        
        // When
        DefaultWorkItem updatedItem = item.withStatus(newStatus);
        
        // Then
        assertThat(updatedItem.getId()).isEqualTo(item.getId());
        assertThat(updatedItem.getTitle()).isEqualTo(item.getTitle());
        assertThat(updatedItem.getType()).isEqualTo(item.getType());
        assertThat(updatedItem.getStatus()).isEqualTo(newStatus);
        assertThat(updatedItem.getCreatedAt()).isEqualTo(item.getCreatedAt());
        assertThat(updatedItem.getUpdatedAt()).isAfter(item.getUpdatedAt());
    }
}