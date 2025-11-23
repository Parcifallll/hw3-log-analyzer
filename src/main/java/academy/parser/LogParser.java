package academy.parser;

import academy.model.Log;

public interface LogParser {
    Log parseLine(String line);
}
