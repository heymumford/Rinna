package com.rinna;

import com.rinna.model.WorkItem;
import com.rinna.model.WorkItemCreateRequest;
import com.rinna.model.WorkItemType;
import com.rinna.model.WorkflowState;
import com.rinna.service.InvalidTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RinnaTest {

    @Test
    void shouldInitializeWithDefaultServices() {
        // When
        Rinna rinna = Rinna.initialize();

        // Then
        assertThat(rinna.items()).isNotNull();
        assertThat(rinna.workflow()).isNotNull();
    }

    @Test
    void shouldCreateItemThroughRinna() {
        // Given
        Rinna rinna = Rinna.initialize();
        WorkItemCreateRequest request = WorkItemCreateRequest.builder()
                .title("Test through Rinna")
                .type(WorkItemType.FEATURE)
                .build();

        // When
        WorkItem item = rinna.items().create(request);

        // Then
        assertThat(item).isNotNull();
        assertThat(item.getTitle()).isEqualTo("Test through Rinna");
        assertThat(item.getType()).isEqualTo(WorkItemType.FEATURE);
    }

    @Test
    void shouldTransitionItemThroughRinna() throws InvalidTransitionException {
        // Given
        Rinna rinna = Rinna.initialize();
        WorkItem item = rinna.items().create(WorkItemCreateRequest.builder()
                .title("Transition Test")
                .type(WorkItemType.BUG)
                .build());

        // When
        WorkItem transitionedItem = rinna.workflow().transition(item.getId(), WorkflowState.IN_PROGRESS);

        // Then
        assertThat(transitionedItem.getStatus()).isEqualTo(WorkflowState.IN_PROGRESS);
    }
}