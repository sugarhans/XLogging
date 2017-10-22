package com.hello2mao.xlogging.io.parser;


import com.hello2mao.xlogging.io.CharBuffer;

public class HttpTrailerParser extends AbstractParser {
    private static final int MAX_LENGTH = 128;
    private static final int INITIAL_LENGTH = 8;
    private boolean foundEmptyLine = false;

    public HttpTrailerParser(AbstractParser parser) {
        super(parser);
    }

    @Override
    public final boolean parse(CharBuffer paramCharBuffer) {
        log.debug("Run parse in HttpTrailerParser");

        if (paramCharBuffer.subStringTrimmed(paramCharBuffer.length).length() == 0) {}
        // FIXME
        for (boolean bool = true;; bool = false) {
            this.foundEmptyLine = bool;
            return true;
        }
    }

    public boolean isFoundEmptyLine() {
        return this.foundEmptyLine;
    }

    public void setFoundEmptyLine(boolean foundEmptyLine) {
        this.foundEmptyLine = foundEmptyLine;
    }


    @Override
    protected int getInitialBufferSize() {
        return INITIAL_LENGTH;
    }

    @Override
    protected int getMaxBufferSize() {
        return MAX_LENGTH;
    }

    @Override
    public AbstractParser nextParserAfterBufferFull() {
        this.buffer.length = 0;
        return new NewlineLineParser(this);
    }

    @Override
    public AbstractParser nextParserAfterSuccessfulParse() {
        if (this.foundEmptyLine) {
            getHandler().finishedMessage(getCharactersInMessage());
            return getHandler().getInitialParser();
        }
        this.buffer.length = 0;
        return this;
    }


}