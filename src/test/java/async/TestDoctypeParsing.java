package async;

import javax.xml.stream.XMLStreamException;

//import async.AsyncTestBase.AsyncReaderWrapper;


import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestDoctypeParsing extends AsyncTestBase
{
    public void testSimplest() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testSimplest(SPC, 1);
            _testSimplest(SPC, 2);
            _testSimplest(SPC, 3);
            _testSimplest(SPC, 5);
            _testSimplest(SPC, 11);
            _testSimplest(SPC, 1000);
        }
    }

    public void testWithSystemId() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);            
            _testWithIds(SPC, 1);
            _testWithIds(SPC, 2);
            _testWithIds(SPC, 3);
            _testWithIds(SPC, 6);
            _testWithIds(SPC, 900);
        }
    }

    public void testParseFull() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testFull(SPC, true, 1);
            _testFull(SPC, true, 2);
            _testFull(SPC, true, 3);
            _testFull(SPC, true, 6);
            _testFull(SPC, true, 900);
        }
    }

    public void testSkipFull() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testFull(SPC, false, 1);
            _testFull(SPC, false, 2);
            _testFull(SPC, false, 3);
            _testFull(SPC, false, 6);
            _testFull(SPC, false, 900);
        }
    }
    
    public void testInvalidDup() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testInvalidDup(SPC, 1);
            _testInvalidDup(SPC, 2);
            _testInvalidDup(SPC, 3);
            _testInvalidDup(SPC, 6);
            _testInvalidDup(SPC, 900);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    private void _testSimplest(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!DOCTYPE root>  <root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        // as per Stax API, can't call getLocalName (ugh), but Stax2 gives us this:
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
    }

    private void _testWithIds(String spaces, int chunkSize) throws Exception
    {
        final String PUBLIC_ID = "some-id";
        final String SYSTEM_ID = "file:/something";
        String XML = spaces+"<!DOCTYPE root PUBLIC '"+PUBLIC_ID+"' \""+SYSTEM_ID+"\"><root/>";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        assertTokenType(DTD, sr.getEventType());
        assertEquals("root", sr.getPrefixedName());
        assertEquals(SYSTEM_ID, sr.getDTDInfo().getDTDSystemId());
        assertEquals(PUBLIC_ID, sr.getDTDInfo().getDTDPublicId());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
        sr.close();
    }

    private void _testFull(String spaces, boolean checkValue, int chunkSize) throws Exception
    {
        final String INTERNAL_SUBSET = "<!--My dtd-->\n"
            +"<!ELEMENT html (head, body)>"
            +"<!ATTLIST head title CDATA #IMPLIED>"
            ;
        final String SYSTEM_ID = "file:/something";
        String XML = spaces+"<!DOCTYPE root SYSTEM '"+SYSTEM_ID+"' ["+INTERNAL_SUBSET+"]>\n<root/>";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        if (checkValue) {
            assertNull(sr.getDTDInfo().getDTDPublicId());
            assertEquals(SYSTEM_ID, sr.getDTDInfo().getDTDSystemId());
            assertEquals("root", sr.getPrefixedName());
            String subset = sr.getText();
            assertEquals(INTERNAL_SUBSET, subset);
        }
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertTokenType(END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
        sr.close();
    }

    private void _testInvalidDup(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!DOCTYPE root> <!DOCTYPE root> <root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        assertEquals("root", sr.getPrefixedName());

        // so far so good, but not any more:
        try {
            reader.nextToken();
        } catch (XMLStreamException e) {
            verifyException(e, "Duplicate DOCTYPE declaration");
        }
        sr.close();
    }

}
