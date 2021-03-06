/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.staff.service;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.mifosplatform.infrastructure.codes.data.CodeValueData;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.staff.data.StaffData;
import org.mifosplatform.organisation.staff.exception.StaffNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Service
public class StaffReadPlatformServiceImpl implements StaffReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final StaffLookupMapper lookupMapper = new StaffLookupMapper();
    private final StaffInOfficeHierarchyMapper staffInOfficeHierarchyMapper = new StaffInOfficeHierarchyMapper();

    @Autowired
    public StaffReadPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource) {
        this.context = context;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final class StaffMapper implements RowMapper<StaffData> {

        public String schema() {
            return " s.id as id,s.office_id as officeId, o.name as officeName, s.firstname as firstname, s.lastname as lastname,"
                    + " s.display_name as displayName, s.is_loan_officer as isLoanOfficer, s.external_id as externalId, s.mobile_no as mobileNo,"
                    + " s.mobile_no2 as mobileNo2, s.email as email, s.address as address, s.emergency_contact_name as emergencyContactName, s.emergency_contact_mobile_no as emergencyContactMobileNo, s.children as children,"
            		+ " s.is_active as isActive, s.joining_date as joiningDate, "
                    + " s.gender_cv_id as genderId, "
                    + " cvgender.code_value as genderValue, "
                    + " s.marital_status_cv_id as maritalStatusId, "
                    + " cvmaritalstatus.code_value as maritalStatusValue, "
                    + " s.emergency_contact_relation_cv_id as emergencyContactRelationId, "
                    + " cvemergencyrelation.code_value as emergencyContactRelationValue "
                    + " from m_staff s "
                    + " join m_office o on o.id = s.office_id"
                    + " left join m_code_value cvgender on cvgender.id = s.gender_cv_id"
                    + " left join m_code_value cvmaritalstatus on cvmaritalstatus.id = s.marital_status_cv_id"
                    + " left join m_code_value cvemergencyrelation on cvemergencyrelation.id = s.emergency_contact_relation_cv_id";
        }

        @Override
        public StaffData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String firstname = rs.getString("firstname");
            final String lastname = rs.getString("lastname");
            final String displayName = rs.getString("displayName");
            final Long officeId = rs.getLong("officeId");
            final boolean isLoanOfficer = rs.getBoolean("isLoanOfficer");
            final String officeName = rs.getString("officeName");
            final String externalId = rs.getString("externalId");
            final String mobileNo = rs.getString("mobileNo");
            final String mobileNo2 = rs.getString("mobileNo2");
            final String email = rs.getString("email");
            final String address = rs.getString("address");
            final Integer children = rs.getInt("children");
            final String emergencyContactName = rs.getString("emergencyContactName");
            final String emergencyContactMobileNo = rs.getString("emergencyContactMobileNo");
            final boolean isActive = rs.getBoolean("isActive");
            final LocalDate joiningDate = JdbcSupport.getLocalDate(rs, "joiningDate");

            final Long genderId = JdbcSupport.getLong(rs, "genderId");
            final String genderValue = rs.getString("genderValue");
            final CodeValueData gender = CodeValueData.instance(genderId, genderValue);

            final Long maritalStatusId = JdbcSupport.getLong(rs, "maritalStatusId");
            final String maritalStatusValue = rs.getString("maritalStatusValue");
            final CodeValueData maritalStatus = CodeValueData.instance(maritalStatusId, maritalStatusValue);

            final Long emergencyContactRelationId = JdbcSupport.getLong(rs, "emergencyContactRelationId");
            final String emergencyContactRelationValue = rs.getString("emergencyContactRelationValue");
            final CodeValueData emergencyContactRelation = CodeValueData.instance(emergencyContactRelationId, emergencyContactRelationValue);

            return StaffData.instance(id, firstname, lastname, displayName, officeId, officeName, isLoanOfficer, externalId, mobileNo,
                    isActive, joiningDate, gender, maritalStatus, emergencyContactRelation, mobileNo2, email, address, children, emergencyContactName, emergencyContactMobileNo);
        }
    }

    private static final class StaffInOfficeHierarchyMapper implements RowMapper<StaffData> {

        public String schema(final boolean loanOfficersOnly) {

            final StringBuilder sqlBuilder = new StringBuilder(200);

            sqlBuilder.append("s.id as id, s.office_id as officeId, ohierarchy.name as officeName,");
            sqlBuilder.append("s.firstname as firstname, s.lastname as lastname,");
            sqlBuilder.append("s.display_name as displayName, s.is_loan_officer as isLoanOfficer, s.external_id as externalId, ");
            sqlBuilder.append("s.mobile_no as mobileNo, s.is_active as isActive, s.joining_date as joiningDate, ");
            sqlBuilder.append("s.mobile_no2 as mobileNo2, s.email as email, s.address as address, s.emergency_contact_name as emergencyContactName, s.emergency_contact_mobile_no as emergencyContactMobileNo, s.children as children,");
            sqlBuilder.append("s.gender_cv_id as genderId, ");
            sqlBuilder.append("cvgender.code_value as genderValue, ");
            sqlBuilder.append("s.marital_status_cv_id as maritalStatusId, ");
            sqlBuilder.append("cvmaritalstatus.code_value as maritalStatusValue, ");
            sqlBuilder.append("s.emergency_contact_relation_cv_id as emergencyContactRelationId, ");
            sqlBuilder.append("cvemergencyrelation.code_value as emergencyContactRelationValue ");
            sqlBuilder.append("from m_office o ");
            sqlBuilder.append("join m_office ohierarchy on o.hierarchy like concat(ohierarchy.hierarchy, '%') ");
            sqlBuilder.append("join m_staff s on s.office_id = ohierarchy.id and s.is_active=1 ");
            sqlBuilder.append("left join m_code_value cvgender on cvgender.id = s.gender_cv_id ");
            sqlBuilder.append("left join m_code_value cvmaritalstatus on cvmaritalstatus.id = s.marital_status_cv_id ");
            sqlBuilder.append("left join m_code_value cvemergencyrelation on cvemergencyrelation.id = s.emergency_contact_relation_cv_id ");

            if (loanOfficersOnly) {
                sqlBuilder.append("and s.is_loan_officer is true ");
            }

            sqlBuilder.append("where o.id = ? ");

            return sqlBuilder.toString();
        }

        @Override
        public StaffData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String firstname = rs.getString("firstname");
            final String lastname = rs.getString("lastname");
            final String displayName = rs.getString("displayName");
            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");
            final boolean isLoanOfficer = rs.getBoolean("isLoanOfficer");
            final String externalId = rs.getString("externalId");
            final String mobileNo = rs.getString("mobileNo");
            final String mobileNo2 = rs.getString("mobileNo2");
            final String email = rs.getString("email");
            final String address = rs.getString("address");
            final Integer children = rs.getInt("children");
            final String emergencyContactName = rs.getString("emergencyContactName");
            final String emergencyContactMobileNo = rs.getString("emergencyContactMobileNo");
            final boolean isActive = rs.getBoolean("isActive");
            final LocalDate joiningDate = JdbcSupport.getLocalDate(rs, "joiningDate");

            final Long genderId = JdbcSupport.getLong(rs, "genderId");
            final String genderValue = rs.getString("genderValue");
            final CodeValueData gender = CodeValueData.instance(genderId, genderValue);

            final Long maritalStatusId = JdbcSupport.getLong(rs, "maritalStatusId");
            final String maritalStatusValue = rs.getString("maritalStatusValue");
            final CodeValueData maritalStatus = CodeValueData.instance(maritalStatusId, maritalStatusValue);

            final Long emergencyContactRelationId = JdbcSupport.getLong(rs, "emergencyContactRelationId");
            final String emergencyContactRelationValue = rs.getString("emergencyContactRelationValue");
            final CodeValueData emergencyContactRelation = CodeValueData.instance(emergencyContactRelationId, emergencyContactRelationValue);

            return StaffData.instance(id, firstname, lastname, displayName, officeId, officeName, isLoanOfficer, externalId, mobileNo,
                    isActive, joiningDate, gender, maritalStatus, emergencyContactRelation, mobileNo2, email, address, children, emergencyContactName, emergencyContactMobileNo);
        }
    }

    private static final class StaffLookupMapper implements RowMapper<StaffData> {

        private final String schemaSql;

        public StaffLookupMapper() {

            final StringBuilder sqlBuilder = new StringBuilder(100);
            sqlBuilder.append("s.id as id, s.display_name as displayName, s.is_loan_officer as isLoanOfficer, s.is_active as isActive ");
            sqlBuilder.append("from m_staff s ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public StaffData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String displayName = rs.getString("displayName");
            final boolean isActive = rs.getBoolean("isActive");
            final boolean isLoanOfficer = rs.getBoolean("isLoanOfficer");
            return StaffData.lookup(id, displayName, isActive, isLoanOfficer);
        }
    }

    @Override
    public Collection<StaffData> retrieveAllLoanOfficersInOfficeById(final Long officeId) {
        return retrieveAllStaff(" office_id=" + officeId + " and is_loan_officer=1");
    }

    @Override
    public Collection<StaffData> retrieveAllStaffForDropdown(final Long officeId) {

        final Long defaultOfficeId = defaultToUsersOfficeIfNull(officeId);

        final String sql = "select " + this.lookupMapper.schema() + " where s.office_id = ? and s.is_active=1 ";

        return this.jdbcTemplate.query(sql, this.lookupMapper, new Object[] { defaultOfficeId });
    }

    private Long defaultToUsersOfficeIfNull(final Long officeId) {
        Long defaultOfficeId = officeId;
        if (defaultOfficeId == null) {
            defaultOfficeId = this.context.authenticatedUser().getOffice().getId();
        }
        return defaultOfficeId;
    }

    @Override
    public StaffData retrieveStaff(final Long staffId) {

        try {
            final StaffMapper rm = new StaffMapper();
            final String sql = "select " + rm.schema() + " where s.id = ?";

            return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { staffId });
        } catch (final EmptyResultDataAccessException e) {
            throw new StaffNotFoundException(staffId);
        }
    }

    @Override
    public Collection<StaffData> retrieveAllStaff(final String sqlSearch, final Long officeId, final boolean loanOfficersOnly,
            final String status) {
        final String extraCriteria = getStaffCriteria(sqlSearch, officeId, loanOfficersOnly, status, false);
        return retrieveAllStaff(extraCriteria);
    }

    @Override
    public Collection<StaffData> retrieveAllStaffWithAssignedGlAccounts() {
        final String extraCriteria = getStaffCriteria(null, null, false, "active", true);
//        final String extraCriteria = "(select count(id) from m_cost_center where staff_id = s.id) > 0";
        return retrieveAllStaff(extraCriteria);
    }

    private Collection<StaffData> retrieveAllStaff(final String extraCriteria) {

        final StaffMapper rm = new StaffMapper();
        String sql = "select " + rm.schema();
        if (StringUtils.isNotBlank(extraCriteria)) {
            sql += " where " + extraCriteria;
        }
        sql = sql + " order by s.lastname";
        return this.jdbcTemplate.query(sql, rm, new Object[] {});
    }

    private String getStaffCriteria(final String sqlSearch, final Long officeId, final boolean loanOfficersOnly, final String status, final boolean includeOnlyWithAssignedGlAccount) {

        final StringBuffer extraCriteria = new StringBuffer(200);

        if (sqlSearch != null) {
            extraCriteria.append(" and (").append(sqlSearch).append(")");
        }
        if (officeId != null) {
            extraCriteria.append(" and office_id = ").append(officeId).append(" ");
        }
        if (loanOfficersOnly) {
            extraCriteria.append(" and s.is_loan_officer is true ");
        }
        if (includeOnlyWithAssignedGlAccount) {
            extraCriteria.append(" and (select count(mcc.staff_id) from m_cost_center mcc where mcc.staff_id = s.id) > 0");
        }
        // Passing status parameter to get ACTIVE (By Default), INACTIVE or ALL
        // (Both active and Inactive) employees
        if (status.equalsIgnoreCase("active")) {
            extraCriteria.append(" and is_active = 1 ");
        } else if (status.equalsIgnoreCase("inActive")) {
            extraCriteria.append(" and is_active = 0 ");
        } else if (status.equalsIgnoreCase("all")) {} else {
            throw new UnrecognizedQueryParamException("status", status, new Object[] { "all", "active", "inactive" });
        }

        if (StringUtils.isNotBlank(extraCriteria.toString())) {
            extraCriteria.delete(0, 4);
        }

        // remove begin four letter including a space from the string.
        return extraCriteria.toString();
    }

    @Override
    public Collection<StaffData> retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(final Long officeId, final boolean loanOfficersOnly) {

        String sql = "select " + this.staffInOfficeHierarchyMapper.schema(loanOfficersOnly);
        sql = sql + " order by s.lastname";
        return this.jdbcTemplate.query(sql, this.staffInOfficeHierarchyMapper, new Object[] { officeId });
    }
}