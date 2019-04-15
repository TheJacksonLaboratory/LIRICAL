package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.exception.Lr2pgException;

public abstract class Lr2PgCommand {

    public abstract void run() throws Lr2pgException;
}
