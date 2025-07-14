package org.mifos.creditbureau.service;

import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@AllArgsConstructor
public class CreditBureauRegistrationReadImplService implements CreditBureauRegistrationReadService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public CBRegisterParamsData getCreditBureauParams(Long creditBureauId) {
        final String sql = "select id, parameter_name, parameter_value from m_credit_bureau_parameter where credit_bureau_id = ?";
        return jdbcTemplate.queryForObject(sql, new CBRegisterParamsDataMapper(), creditBureauId);
    }

    @Override
    public List<CreditBureauData> getAllCreditBureaus() {
        final String sql = "select id, name, is_active, is_available, country from m_credit_bureau";
        return jdbcTemplate.query(sql, new CreditBureauDataMapper());
    }

    private static final class CreditBureauDataMapper implements RowMapper<CreditBureauData> {
        @Override
        public CreditBureauData mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            boolean isActive = rs.getBoolean("is_active");
            boolean isAvailable = rs.getBoolean("is_available");
            String country = rs.getString("country");
            return new CreditBureauData(id, name, isAvailable, isActive, country, null);
        }
    }

    private static final class CBRegisterParamsDataMapper implements RowMapper<CBRegisterParamsData> {
        @Override
        public CBRegisterParamsData mapRow(ResultSet rs, int rowNum) throws SQLException {
            CBRegisterParamsData paramsData = new CBRegisterParamsData();
            paramsData.setId(rs.getLong("id"));
            do {
                paramsData.setParam(rs.getString("parameter_name"), rs.getString("parameter_value"));
            } while (rs.next());
            return paramsData;
        }
    }
}
