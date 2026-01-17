package de.cavdar.itsq;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeFinderFunction implements BiFunction<String, String, Boolean> {

    private final List<Pattern> patternList = new ArrayList<>();
    private final List<String> xmlTagsList = new ArrayList<>();

    public DateTimeFinderFunction(List<String> xmlTagsList, List<Pattern> patternList) {
        this.patternList.addAll(patternList);
        this.xmlTagsList.addAll(xmlTagsList);
    }

    public DateTimeFinderFunction(List<String> xmlTagsList) {
        this.xmlTagsList.addAll(xmlTagsList);
    }

    @Override
    public Boolean apply(String xmlTagName, String xmlTagValue) {
        if (xmlTagsList.isEmpty()) {
            return checkForPatterns(xmlTagValue);
        }
        return xmlTagsList.contains(xmlTagName);
    }

    private Boolean checkForPatterns(String xmlTagValue) {
        for (Pattern pattern : patternList) {
            Matcher matcher = pattern.matcher(xmlTagValue);
            if (matcher.find()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}
