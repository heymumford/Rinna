package com.rinna.service.impl;

import com.rinna.model.Priority;
import com.rinna.model.WorkItem;
import com.rinna.model.WorkItemCreateRequest;
import com.rinna.model.WorkItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryItemServiceTest {

    private InMemoryItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new InMemoryItemService();
    }

    @Test
    void shouldCreateItem() {
        // Given
        WorkItemCreateRequest request = WorkItemCreateRequest.builder()
                .title("Test Bug")
                .type(WorkItemType.BUG)
                .build();

        // When
        WorkItem createdItem = itemService.create(request);

        // Then
        assertThat(createdItem).isNotNull();
        assertThat(createdItem.getId()).isNotNull();
        assertThat(createdItem.getTitle()).isEqualTo("Test Bug");
        assertThat(createdItem.getType()).isEqualTo(WorkItemType.BUG);
    }

    @Test
    void shouldFindItemById() {
        // Given
        WorkItem item = itemService.create(WorkItemCreateRequest.builder()
                .title("Test Bug")
                .type(WorkItemType.BUG)
                .build());

        // When
        Optional<WorkItem> foundItem = itemService.findById(item.getId());

        // Then
        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getId()).isEqualTo(item.getId());
    }

    @Test
    void shouldReturnEmptyWhenItemNotFound() {
        // When
        Optional<WorkItem> foundItem = itemService.findById(UUID.randomUUID());

        // Then
        assertThat(foundItem).isEmpty();
    }

    @Test
    void shouldFindAllItems() {
        // Given
        itemService.clear();
        WorkItem bug = itemService.create(WorkItemCreateRequest.builder()
                .title("Test Bug")
                .type(WorkItemType.BUG)
                .build());
        WorkItem feature = itemService.create(WorkItemCreateRequest.builder()
                .title("Test Feature")
                .type(WorkItemType.FEATURE)
                .build());

        // When
        List<WorkItem> allItems = itemService.findAll();

        // Then
        assertThat(allItems).hasSize(2);
        assertThat(allItems).extracting(WorkItem::getId)
                .containsExactlyInAnyOrder(bug.getId(), feature.getId());
    }

    @Test
    void shouldUpdateItem() {
        // Given
        WorkItem item = itemService.create(WorkItemCreateRequest.builder()
                .title("Test Bug")
                .type(WorkItemType.BUG)
                .priority(Priority.MEDIUM)
                .build());

        // When
        WorkItem updatedItem = itemService.update(item);

        // Then
        assertThat(updatedItem).isNotNull();
        assertThat(updatedItem.getId()).isEqualTo(item.getId());
    }

    @Test
    void shouldClearAllItems() {
        // Given
        itemService.create(WorkItemCreateRequest.builder()
                .title("Test Bug")
                .type(WorkItemType.BUG)
                .build());
        itemService.create(WorkItemCreateRequest.builder()
                .title("Test Feature")
                .type(WorkItemType.FEATURE)
                .build());

        // When
        itemService.clear();

        // Then
        assertThat(itemService.findAll()).isEmpty();
    }
}