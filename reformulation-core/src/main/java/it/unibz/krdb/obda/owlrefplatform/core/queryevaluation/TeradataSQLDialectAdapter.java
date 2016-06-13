package it.unibz.krdb.obda.owlrefplatform.core.queryevaluation;

import java.sql.Types;

public class TeradataSQLDialectAdapter extends SQL99DialectAdapter {
    @Override
    public String sqlCast(String value, int type) {
        String strType = null;
        if (type == Types.VARCHAR) {
            strType = "VARCHAR(8)";
        } else {
            throw new RuntimeException("Unsupported SQL type");
        }
        return "CAST(" + value + " AS " + strType + ")";
    }

}
