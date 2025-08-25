package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.model.application.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idStatus", ignore = true)
    Application toEntity(RegisterApplicationRequestDto registerApplicationRequestDto);

    ApplicationDto toResponse(Application application);
}
