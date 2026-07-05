package com.example.taskmanager.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startupSeedsExpectedTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.tasks[0].id").exists())
                .andExpect(jsonPath("$.tasks[0].created_at").exists())
                .andExpect(jsonPath("$.tasks[0].updated_at").exists());
    }

    @Test
    void filtersTasksByStatus() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].status").value("pending"));
    }

    @Test
    void rejectsInvalidStatusQueryParameter() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "blocked"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Invalid status: blocked")));
    }

     @Test
     void canCreateAndDeleteTask() throws Exception {
         String requestBody = """
                 {
                   "title": "Write API docs",
                   "author": "Carol",
                   "description": "Document all task endpoints.",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending"
                 }
                 """;

         String createdTaskJson = mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$.id").exists())
                 .andExpect(jsonPath("$.title").value("Write API docs"))
                 .andExpect(jsonPath("$.project.id").value("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"))
                 .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                 .andReturn()
                 .getResponse()
                 .getContentAsString();

         JsonNode createdTask = objectMapper.readTree(createdTaskJson);
         String taskId = createdTask.get("id").asText();

         mockMvc.perform(delete("/api/tasks/{id}", taskId))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.success").value("true"));
     }

     @Test
     void rejectsUnknownStatusWhenProvided() throws Exception {
         String requestBody = """
                 {
                   "title": "Write API docs",
                   "author": "Carol",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "blocked"
                 }
                 """;

         mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.detail", containsString("Invalid status: blocked")));
     }

     @Test
     void returnsValidationMessageWhenTitleIsMissing() throws Exception {
         String requestBody = """
                 {
                   "author": "Carol",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending"
                 }
                 """;

         mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.detail").value("title is required"));
     }

     @Test
     void returnsValidationMessageWhenAuthorIsMissing() throws Exception {
         String requestBody = """
                 {
                   "title": "Write API docs",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending"
                 }
                 """;

         mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.detail").value("author is required"));
     }

     @Test
     void returnsValidationMessageWhenProjectIsMissing() throws Exception {
         String requestBody = """
                 {
                   "title": "Write API docs",
                   "author": "Carol",
                   "status": "pending"
                 }
                 """;

         mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.detail").value("project is required"));
     }

     @Test
     void rejectsUnknownParameterWhenProvided() throws Exception {
         String requestBody = """
                 {
                   "title": "Write API docs",
                   "author": "Carol",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending",
                   "priority": "high"
                 }
                 """;

         mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.detail", containsString("Unrecognized field \"priority\"")));
     }

     @Test
     void returnsNotFoundMessageForMissingTaskOnGetPutDelete() throws Exception {
         String missingId = "9f60ca37-a5c3-4d7c-a8c9-9b5f42c96d11";
         String requestBody = """
                 {
                   "title": "Write API docs",
                   "author": "Carol",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending"
                 }
                 """;

         assertNotFoundWithDetail(
                 mockMvc.perform(get("/api/tasks/{id}", missingId)),
                 "Task not found: " + missingId
         );

         assertNotFoundWithDetail(
                 mockMvc.perform(put("/api/tasks/{id}", missingId)
                         .header("ETag", "\"any\"")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody)),
                 "Task not found: " + missingId
         );

         assertNotFoundWithDetail(
                 mockMvc.perform(delete("/api/tasks/{id}", missingId)),
                 "Task not found: " + missingId
         );
     }

    @Test
    void returnsInvalidIdMessageWhenUuidPathVariableIsMalformed() throws Exception {
        String invalidId = "123e4567-e89b-12d3-a456-426614174000y";

        assertBadRequestWithDetail(
                mockMvc.perform(get("/api/tasks/{id}", invalidId)),
                "Invalid 'id' format: " + invalidId + ". Expected UUID."
        );
    }

     @Test
     void rejectsDuplicateTitleAndAuthorOnCreate() throws Exception {
         // Seed already has "Implement User Authentication" by "Alice Johnson"
         String requestBody = """
                 {
                   "title": "Implement User Authentication",
                   "author": "Alice Johnson",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending"
                 }
                 """;

         mockMvc.perform(post("/api/tasks")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isConflict())
                 .andExpect(jsonPath("$.detail").value(
                         "A task with title 'Implement User Authentication' by 'Alice Johnson' already exists"));
     }

     @Test
     void rejectsDuplicateTitleAndAuthorOnUpdate() throws Exception {
         // Try to update Bob's task to match Alice's task
         String existingId = findTaskId("Design Database Schema", "Bob Smith");
         String requestBody = """
                 {
                   "title": "Implement User Authentication",
                   "author": "Alice Johnson",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "in-progress"
                 }
                 """;

         mockMvc.perform(put("/api/tasks/{id}", existingId)
                         .header("ETag", getTaskEtag(existingId))
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isConflict())
                 .andExpect(jsonPath("$.detail").value(
                         "A task with title 'Implement User Authentication' by 'Alice Johnson' already exists"));
     }

     @Test
     void allowsUpdatingTaskWithSameTitleAndAuthor() throws Exception {
         // Updating a task with its own title+author must succeed
         String existingId = findTaskId("Implement User Authentication", "Alice Johnson");
         String requestBody = """
                 {
                   "title": "Implement User Authentication",
                   "author": "Alice Johnson",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "completed"
                 }
                 """;

         mockMvc.perform(put("/api/tasks/{id}", existingId)
                         .header("ETag", getTaskEtag(existingId))
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestBody))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.status").value("completed"));

         // restore original state
         String restore = """
                 {
                   "title": "Implement User Authentication",
                   "author": "Alice Johnson",
                   "project": {
                     "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
                   },
                   "status": "pending"
                 }
                 """;
         mockMvc.perform(put("/api/tasks/{id}", existingId)
                 .header("ETag", getTaskEtag(existingId))
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(restore));
     }

    private String getTaskEtag(String id) throws Exception {
        return mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private String findTaskId(String title, String author) throws Exception {
        String body = mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        for (JsonNode task : root.get("tasks")) {
            if (title.equals(task.get("title").asText()) && author.equals(task.get("author").asText())) {
                return task.get("id").asText();
            }
        }
        throw new AssertionError("Task not found for title/author: " + title + " / " + author);
    }

    private void assertNotFoundWithDetail(ResultActions result, String expectedDetail) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(expectedDetail));
    }

    private void assertBadRequestWithDetail(ResultActions result, String expectedDetail) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(expectedDetail));
    }
}
