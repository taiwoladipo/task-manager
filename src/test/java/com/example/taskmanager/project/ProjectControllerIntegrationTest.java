package com.example.taskmanager.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startupSeedsDefaultProject() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.projects[*].name", hasItem("Tasks API")))
                .andExpect(jsonPath("$.projects[0].created_at").exists());
    }

    @Test
    void canCreateProject() throws Exception {
        String requestBody = """
                {
                  "name": "Billing"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Billing"))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void rejectsCreateProjectWhenNameIsEmpty() throws Exception {
        String requestBody = """
                {
                  "name": "   "
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("name is required"));
    }
}

