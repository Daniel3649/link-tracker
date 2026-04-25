package backend.academy.linktracker.contract.dto.response;

import java.util.List;

public record ListTagsResponse(List<String> tags, Integer size) {}
