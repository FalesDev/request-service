package co.com.pragma.webclient.dto;

import java.util.List;
import java.util.UUID;

public record UsersFoundRequest(
        List<UUID> userIds
) {
}
