package com.ibm.airlock.utilities;

import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.FeatureItem;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to hold filtering conditions for features
 * and check if feature satisfies filtering conditions
 */
public class FeatureFilter {

    public static final Logger logger = Logger.getLogger(FeatureFilter.class.getName());

    public enum SearchArea {
        NAME,
        NAMESPACE,
        CONFIGURATION,
        RULE,
        DESCRIPTION,
        DISPLAY_NAME
    }

    public enum SearchOption {
        // Indicates if search is case sensitive
        CASE_SENSITIVE,

        // If flag is set then search features where search area completely matches pattern
        // e.g. if pattern is "test" (and area is name) then only feature with name "test" will be returned
        // If flag is not set then pattern can be anywhere in search area
        // e.g. if pattern is "test" (and area is name) then next features may be found "test1", "mytest", "mytestnew", etc.
        EXACT_MATCH,

        // If flag is set then pattern will be treated as Java Regular Expression
        // e.g user will be able to search patterns like [a-z]*
        // if flag is not set then pattern will allow only wildcard symbols like ? and *
        REG_EXP,
    }

    // This is empty filter. In case we don't want to do any filtering at all.
    public static final FeatureFilter NONE = new FeatureFilter();

    private Set<SearchArea> searchAreas;
    private Set<SearchOption> searchOptions;
    private String pattern;

    // Just evaluate pattern once during class creating and store here
    private Pattern compiledPattern;

    // indicates if we should search in all areas
    private boolean searchInAllAreas = false;

    private FeatureFilter() {
    }

    public FeatureFilter(String pattern, Set<SearchArea> searchAreas, Set<SearchOption> searchOptions) {
        this.pattern = pattern;
        this.searchAreas = searchAreas;
        this.searchOptions = searchOptions;
        compilePattern();
    }

    public static FeatureFilter build(String pattern, Set<String> searchAreas, Set<String> searchOptions) {
        return new FeatureFilter(pattern, strToSearchAreas(searchAreas), strToSearchOptions(searchOptions));
    }

    public boolean isConditionSatisfied(BaseAirlockItem baseAirlockItem) {

        // no pattern - no filtering
        if (pattern == null) {
            return true;
        }

        // Don't apply filter if this is not a Feature
        if (baseAirlockItem instanceof FeatureItem) {
            FeatureItem featureItem = (FeatureItem) baseAirlockItem;

            if (searchInAllAreas || searchAreas.contains(SearchArea.NAME)) {
                Matcher m = compiledPattern.matcher(featureItem.getName());
                if (m.matches()) {
                    return true;
                }
            }

            if ((searchInAllAreas || searchAreas.contains(SearchArea.NAMESPACE))
                    && featureItem.getNamespace() != null) {
                Matcher m = compiledPattern.matcher(featureItem.getNamespace());
                if (m.matches()) {
                    return true;
                }
            }

            if ((searchInAllAreas || searchAreas.contains(SearchArea.CONFIGURATION))
                    && featureItem.getDefaultConfiguration() != null) {
                Matcher m = compiledPattern.matcher(featureItem.getDefaultConfiguration());
                if (m.matches()) {
                    return true;
                }
            }

            if ((searchInAllAreas || searchAreas.contains(SearchArea.RULE))
                    && featureItem.getRule() != null
                    && featureItem.getRule().getRuleString() != null) {
                Matcher m = compiledPattern.matcher(featureItem.getRule().getRuleString());
                if (m.matches()) {
                    return true;
                }
            }

            if ((searchInAllAreas || searchAreas.contains(SearchArea.DESCRIPTION))
                    && featureItem.getDescription() != null) {
                Matcher m = compiledPattern.matcher(featureItem.getDescription());
                if (m.matches()) {
                    return true;
                }
            }

            if ((searchInAllAreas || searchAreas.contains(SearchArea.DISPLAY_NAME))
                    && featureItem.getDisplayName() != null) {
                Matcher m = compiledPattern.matcher(featureItem.getDisplayName());
                if (m.matches()) {
                    return true;
                }
            }

        } else {
            return false;
        }

        return false;
    }

    // Compile pattern based on search options
    private void compilePattern() {

        if (pattern == null) {
            return;
        }

        // if search areas are not specified then search in all areas
        searchInAllAreas = searchAreas == null || searchAreas.isEmpty();

        String p = pattern;

        if (!searchOptions.contains(SearchOption.REG_EXP)) {
            p = p.replaceAll("\\*", ".*");
            p = p.replaceAll("\\?", ".");
        }

        if (!searchOptions.contains(SearchOption.EXACT_MATCH)) {
            p = ".*" + p + ".*";
        }

        if (!searchOptions.contains(SearchOption.CASE_SENSITIVE)) {
            p = "(?i)" + p;
        }
        compiledPattern = Pattern.compile(p);
    }

    public static Set<SearchArea> strToSearchAreas(Set<String> searchAreas) {
        Set<SearchArea> searchAreasSet = new HashSet<>();
        if (searchAreas != null) {
            for (String searchArea : searchAreas) {
                try {
                    searchAreasSet.add(FeatureFilter.SearchArea.valueOf(searchArea.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Can't parse search area: " + searchArea);
                }
            }
        }
        return searchAreasSet;
    }

    public static Set<SearchOption> strToSearchOptions(Set<String> searchOptions) {
        Set<SearchOption> searchOptionsSet = new HashSet<>();
        if (searchOptions != null) {
            for (String searchOption : searchOptions) {
                try {
                    searchOptionsSet.add(FeatureFilter.SearchOption.valueOf(searchOption.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Can't parse search option: " + searchOption);
                }
            }
        }
        return searchOptionsSet;
    }

}
