package kirok.querydsl.repository;

import java.util.List;
import kirok.querydsl.dto.MemberSearchDto;
import kirok.querydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchDto condition);

    Page<MemberTeamDto> searchPageSimple(MemberSearchDto condition, Pageable pageable);

    Page<MemberTeamDto> searchPageComplex(MemberSearchDto condition, Pageable pageable);
}
