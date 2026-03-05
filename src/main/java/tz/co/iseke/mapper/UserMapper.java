package tz.co.iseke.mapper;

import tz.co.iseke.inputs.CreateUserInput;
import tz.co.iseke.inputs.UpdateUserInput;
import tz.co.iseke.dto.UserDto;
import tz.co.iseke.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(CreateUserInput input);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromInput(UpdateUserInput input, @MappingTarget User entity);

    UserDto toDto(User entity);
}