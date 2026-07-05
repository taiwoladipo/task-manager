package com.example.taskmanager.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OptimisticLockingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getTaskReturnsEtagHeaderAndDoesNotExposeVersion() throws Exception {
        UUID seedTaskId = resolveAnyTaskId();
        mockMvc.perform(get("/api/tasks/{id}", seedTaskId))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void updateSucceedsWhenEtagMatches() throws Exception {
        UUID seedTaskId = resolveAnyTaskId();
        String etag = getTaskEtag(seedTaskId);

        String updateBody = """
                {
                  "title": "Implement User Authentication",
                  "author": "Alice Johnson",
                  "description": "Updated with matching etag.",
                  "project": {
                    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                  },
                  "status": "in-progress"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", seedTaskId)
                        .header("ETag", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("ETag", not(etag)))
                .andExpect(jsonPath("$.description").value("Updated with matching etag."));

        restoreSeedTask(seedTaskId);
    }

    @Test
    void updateFailsWhenEtagIsStale() throws Exception {
        UUID seedTaskId = resolveAnyTaskId();
        String staleEtag = getTaskEtag(seedTaskId);

        String firstUpdate = """
                {
                  "title": "Implement User Authentication",
                  "author": "Alice Johnson",
                  "description": "First update description.",
                  "project": {
                    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                  },
                  "status": "in-progress"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", seedTaskId)
                        .header("ETag", staleEtag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstUpdate))
                .andExpect(status().isOk());

        String staleUpdate = """
                {
                  "title": "Implement User Authentication",
                  "author": "Alice Johnson",
                  "description": "Stale update attempt.",
                  "project": {
                    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                  },
                  "status": "completed"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", seedTaskId)
                        .header("ETag", staleEtag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staleUpdate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Task has been updated by another user.")));

        restoreSeedTask(seedTaskId);
    }

    @Test
    void updateRequiresEtagHeader() throws Exception {
        UUID seedTaskId = resolveAnyTaskId();
        String updateBody = """
                {
                  "title": "Implement User Authentication",
                  "author": "Alice Johnson",
                  "description": "No etag update attempt.",
                  "project": {
                    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                  },
                  "status": "in-progress"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", seedTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("ETag")));
    }

    private String getTaskEtag(UUID id) throws Exception {
        return mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private UUID resolveAnyTaskId() throws Exception {
        String response = mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        JsonNode tasks = root.get("tasks");
        if (tasks == null || !tasks.isArray() || tasks.isEmpty()) {
            throw new AssertionError("Expected at least one seeded task");
        }
        return UUID.fromString(tasks.get(0).get("id").asText());
    }

    private void restoreSeedTask(UUID seedTaskId) throws Exception {
        String etag = getTaskEtag(seedTaskId);
        String restore = """
                {
                  "title": "Implement User Authentication",
                  "author": "Alice Johnson",
                  "description": "Create a secure user authentication system using JWT.",
                  "project": {
                    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                  },
                  "status": "pending"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", seedTaskId)
                        .header("ETag", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restore))
                .andExpect(status().isOk());
    }
}

