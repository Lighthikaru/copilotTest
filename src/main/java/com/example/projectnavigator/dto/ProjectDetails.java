package com.example.projectnavigator.dto;

import java.util.List;

public record ProjectDetails(
        ProjectConnection connection,
        ProjectIndex index,
        List<JobStatus> recentJobs) {
}
