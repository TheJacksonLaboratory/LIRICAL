package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.exception.LiricalException;

import java.io.File;

public abstract class LiricalCommand {

    public abstract void run() throws LiricalException;

}
