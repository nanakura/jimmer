package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;

import java.sql.Connection;

class AssociationSaveCommandImpl implements AssociationSaveCommand {

    private AssociationExecutable executable;

    public AssociationSaveCommandImpl(AssociationExecutable executable) {
        this.executable = executable;
    }

    @Override
    public Integer execute() {
        return executable
                .sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public Integer execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return executable
                .sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    private Integer executeImpl(Connection con) {
        return executable.execute(con);
    }

    @Override
    public AssociationSaveCommand checkExistence() {
        AssociationExecutable newExecutable =
                executable.setMode(AssociationExecutable.Mode.CHECK_AND_INSERT);
        if (newExecutable == executable) {
            return this;
        }
        return new AssociationSaveCommandImpl(newExecutable);
    }
}
