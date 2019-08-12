/*
 * IBM Confidential OCO Source Materials
 *
 * 5725-I43 Copyright IBM Corp. 2006, 2016
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
var window = {};
var Buffer = {};

window.config = {};
window.config.origRandomFunc = Math.random;
window.config.CONSTANT_SEED = 0.7;

function generateFromManySchemas(mainSchemaStr, refSchemaStrArr) {
	
	var mainSchema = JSON.parse(mainSchemaStr);
    
	var refSchemaArr = [];
	for (i = 0; i < refSchemaStrArr.length; i++) {
		refSchemaArr.push(JSON.parse(refSchemaStrArr[i]));
	}
	
	return JSON.stringify(window.jsf(mainSchema, refSchemaArr));
}

function setSeed(seed) {
	Math.random = function() {return seed;}
}

function generate(schemaStr) {
	var schema = JSON.parse(schemaStr);
	return JSON.stringify(window.jsf(schema));
}

function generateWithConstantSeed(schemaStr) {
	var schema = JSON.parse(schemaStr);
	setSeed(window.config.CONSTANT_SEED);
	return JSON.stringify(window.jsf(schema));
}

function generateWithSeed(schemaStr, seed) {
	var schema = JSON.parse(schemaStr);
	setSeed(seed);
	return JSON.stringify(window.jsf(schema));
}

function getRandomSeed() {
	return window.config.origRandomFunc();
}

function simpleGenerate(schema)
{
	return JSON.stringify(window.jsf(JSON.parse(schema)));
}

// mode is "MAXIMAL", "MINIMAL" or "PARTIAL"

// randomizer is -1 if true randomization is required (uses the original Math.random)
// otherwise it is a constant in the range [0,1) that will always be used
// instead of a random number, producing deterministic results in the JSON faker.

function generateWithPruning(schemaStr, stage, minAppVer, mode, randomizer)
{
	if (randomizer < 0)	{
		Math.random = window.config.origRandomFunc;
	}
	else if (randomizer < 1) {
		setSeed(randomizer)
	}
	else
		throw "invalid randomizer " + randomizer;

	var schema = JSON.parse(schemaStr);
//	pruneNulls(schema);
	if (stage != null && minAppVer != null)
		pruneSchema(schema, stage, minAppVer);
	pruneNulls(schema);

	if (mode == "MAXIMAL") {
		forceRequired(schema);
		maximalArrays(schema);
	}
	else if (mode == "MINIMAL") {
		removeOptional(schema);
		minimalArrays(schema);
	}

	return JSON.stringify(window.jsf(schema));
}
