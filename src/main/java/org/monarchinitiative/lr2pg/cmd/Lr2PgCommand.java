package org.monarchinitiative.lr2pg.cmd;

import org.monarchinitiative.lr2pg.exception.Lr2pgException;

public abstract class Lr2PgCommand {

    public abstract void run() throws Lr2pgException;
}
