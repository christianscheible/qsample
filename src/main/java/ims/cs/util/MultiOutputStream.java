/*
 * This file is part of QSample.
 * QSample is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QSample is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QSample.  If not, see <http://www.gnu.org/licenses/>.
 */


package ims.cs.util;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An extension of the default output stream that provides functionality to write to multiple streams at once.
 * We can use this to "tee" standard out and standard error into a file, which makes for a cheap and somewhat dirty
 * logging alternative.
 *
 * Adapted from http://www.codeproject.com/Tips/315892/A-quick-and-easy-way-to-direct-Java-System-out-to
 */
public class MultiOutputStream extends OutputStream
{

    OutputStream[] outputStreams;

    public MultiOutputStream(OutputStream... outputStreams)
    {
        this.outputStreams= outputStreams;
    }

    @Override
    public void write(int b) throws IOException
    {
        for (OutputStream out: outputStreams)
            out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        for (OutputStream out: outputStreams)
            out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        for (OutputStream out: outputStreams)
            out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        for (OutputStream out: outputStreams)
            out.flush();
    }

    @Override
    public void close() throws IOException
    {
        for (OutputStream out: outputStreams)
            out.close();
    }

    /**
     * Write stdout and stderr to two separate files
     * @param fnOut
     * @param fnErr
     */
    public static void init(String fnOut, String fnErr) {
        System.out.println("Logging stdout to: " + fnOut);
        System.out.println("Logging stdout to: " + fnErr);

        try
        {
            FileOutputStream fout= new FileOutputStream(fnOut);
            FileOutputStream ferr= new FileOutputStream(fnErr);

            MultiOutputStream multiOut= new MultiOutputStream(System.out, fout);
            MultiOutputStream multiErr= new MultiOutputStream(System.err, ferr);

            PrintStream stdout= new PrintStream(multiOut);
            PrintStream stderr= new PrintStream(multiErr);

            System.setOut(stdout);
            System.setErr(stderr);
        }
         catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Write stdout and stderr into the same file
     * @param fnOutAndErr
     */
    public static void init(String fnOutAndErr) {
        System.out.println("Logging all output to: " + fnOutAndErr);

        try
        {
            FileOutputStream fout= new FileOutputStream(fnOutAndErr);

            MultiOutputStream multiOut= new MultiOutputStream(System.out, fout);

            PrintStream stdout= new PrintStream(multiOut);

            System.setOut(stdout);
            System.setErr(stdout);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
