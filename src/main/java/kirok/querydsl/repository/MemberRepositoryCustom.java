package kirok.querydsl.repository;

import java.util.List;
import kirok.querydsl.dto.MemberSearchDto;
import kirok.querydsl.dto.MemberTeamDto;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchDto condition);
}
