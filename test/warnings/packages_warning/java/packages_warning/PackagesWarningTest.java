package packages_warning;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import test_utils.ZserioWarnings;

public class PackagesWarningTest
{
    @BeforeClass
    public static void readZserioWarnings() throws IOException
    {
        zserioWarnings = new ZserioWarnings();
    }

    @Test
    public void duplicatedPackageImport()
    {
        final String warning = ":6:1: Duplicated import of package packages_warning.simple_database.";
        assertTrue(zserioWarnings.isPresent(warning));
    }

    @Test
    public void duplicatedSingleTypeImport()
    {
        final String warning = ":6:1: Duplicated import of type packages_warning.simple_database.SimpleTable.";
        assertTrue(zserioWarnings.isPresent(warning));
    }

    @Test
    public void packageImportOverwrite()
    {
        final String warning =
                ":6:1: Import of package packages_warning.simple_database overwrites some single type imports.";
        assertTrue(zserioWarnings.isPresent(warning));
    }

    @Test
    public void singleTypeAlreadyImported()
    {
        final String warning = ":6:1: Single type SimpleTable imported already by package import.";
        assertTrue(zserioWarnings.isPresent(warning));
    }

    @Test
    public void checkNumberOfWarnings()
    {
        final int expectedNumberOfWarnings = 4;
        assertEquals(expectedNumberOfWarnings, zserioWarnings.getCount());
    }

    private static ZserioWarnings zserioWarnings;
}
