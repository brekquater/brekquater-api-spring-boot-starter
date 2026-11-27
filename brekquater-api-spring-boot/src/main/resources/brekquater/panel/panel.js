// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater
//
// Shows the Brekquater API assessment, either as a panel inside Swagger UI or as the dashboard page.
// Plain JavaScript on purpose: no build step, no dependencies. Every value from the assessment or the API document
// is inserted as text, never as HTML, so nothing in the API document can run as code here.
(function () {
  "use strict";

  var SCRIPT = document.currentScript;
  var BASE = SCRIPT.src.replace(/\/panel\.js(\?.*)?$/, "");
  var RULE_PAGES = "https://starter.brekquater.com/api/rules/";
  var STANDALONE_ROOT_ID = "brekquater-panel";
  var SWAGGER_UI_ANCHOR = ".swagger-ui .information-container";
  var SWAGGER_UI_WAIT_MS = 10000;
  // Must match the "beside Swagger UI" media query in panel.css.
  var ASIDE = window.matchMedia("(min-width: 1200px)");
  var THEME_KEY = "brekquater-theme";
  var THEMES = ["system", "light", "dark"];
  var THEME_NAMES = { system: "System", light: "Light", dark: "Dark" };
  var SEVERITIES = [
    { key: "BLOCKER", label: "Blocker" },
    { key: "MAJOR", label: "Major" },
    { key: "MINOR", label: "Minor" },
    { key: "INFO", label: "Info" }
  ];
  var HTTP_METHODS = ["get", "put", "post", "delete", "options", "head", "patch", "trace"];
  var CATEGORY_NAMES = {
    DESIGN: "Design", DOCUMENTATION: "Documentation", SECURITY: "Security", ERRORS: "Errors",
    NAMING: "Naming", TESTING: "Testing", CODE_QUALITY: "Code quality", ARCHITECTURE: "Architecture"
  };
  var GRADE_WORDS = { A: "Excellent", B: "Good", C: "Fair", D: "Weak", F: "Poor" };
  // Colour bands for category scores and coverage; they only pick a colour, they are not part of the scoring.
  var GOOD_FROM = 80;
  var FAIR_FROM = 60;
  // Each vulnerability rule checks one CVSS band; its chip uses the colour of the rule's severity.
  var VULNERABILITY_BANDS = [
    { ruleId: "BQ-VUL-001", label: "Critical", chip: "bq-blocker" },
    { ruleId: "BQ-VUL-002", label: "High", chip: "bq-major" },
    { ruleId: "BQ-VUL-003", label: "Medium", chip: "bq-minor" },
    { ruleId: "BQ-VUL-004", label: "Low", chip: "bq-info" }
  ];
  var REPORT_INPUTS = ["COVERAGE", "TEST_RESULTS", "VULNERABILITIES", "BASELINE"];
  var EVIDENCE_SOURCES = { COVERAGE: "Coverage report", TEST_RESULTS: "Test", VULNERABILITIES: "Dependency" };
  var NOT_ASSESSED_REASONS = {
    MISSING_INPUT: "Input not found",
    NOT_IMPLEMENTED: "Not assessed yet"
  };
  var NOT_ASSESSED_MESSAGE = "No OpenAPI document was found, so the API was not assessed.";
  var NO_FILTER = { severity: "", category: "", text: "" };

  function element(tag, className, text) {
    var node = document.createElement(tag);
    if (className) {
      node.className = className;
    }
    if (text !== undefined && text !== null) {
      node.textContent = String(text);
    }
    return node;
  }

  function append(parent, children) {
    children.forEach(function (child) {
      if (child) {
        parent.appendChild(child);
      }
    });
    return parent;
  }

  // The Brekquater mark, simplified so it stays readable at small sizes. The letter uses the text colour, so it
  // follows the theme; the sun is always gold.
  var MARK_VIEW_BOX = "0 0 100 85";
  var MARK_STROKES = [
    { d: "M6 90V6H38C51 6 58 13 58 21C58 27 55 31 50 34", width: 11 },
    { d: "M55 41H73C86 41 93.5 50 93.5 60C93.5 71 86 79.5 73 79.5H66", width: 11 },
    { d: "M-2 90L39 47L66 72", width: 9 }
  ];
  var MARK_WAVE = "M14 85C23 75 32 70 42 70C54 70 61 78 70 78.5L80 78L80 85Z";
  var MARK_SUN_GOLD = "#c89b5b";

  function mark(sizeClass) {
    var svgNs = "http://www.w3.org/2000/svg";
    var svg = document.createElementNS(svgNs, "svg");
    svg.setAttribute("viewBox", MARK_VIEW_BOX);
    svg.setAttribute("class", "bq-mark " + sizeClass);
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    MARK_STROKES.forEach(function (stroke) {
      svg.appendChild(svgShape("path", { d: stroke.d, fill: "none", stroke: "currentColor",
        "stroke-width": stroke.width }));
    });
    svg.appendChild(svgShape("path", { d: MARK_WAVE, fill: "currentColor" }));
    svg.appendChild(svgShape("circle", { cx: 86, cy: 13, r: 12, fill: MARK_SUN_GOLD }));
    return svg;
  }

  function svgShape(tag, attributes) {
    var shape = document.createElementNS("http://www.w3.org/2000/svg", tag);
    Object.keys(attributes).forEach(function (name) { shape.setAttribute(name, attributes[name]); });
    return shape;
  }

  // ---- Reading the assessment and the document ---------------------------------------------------------------

  function fetchJson(path) {
    return fetch(BASE + path, { headers: { Accept: "application/json" }, credentials: "same-origin" })
      .then(function (response) {
        if (response.status === 401) {
          return { problem: "Sign in to see the API assessment." };
        }
        if (!response.ok) {
          return { problem: "The API assessment could not be loaded (HTTP " + response.status + ")." };
        }
        return response.json().then(function (data) {
          return { data: data };
        });
      })
      .catch(function () {
        return { problem: "The API assessment could not be loaded." };
      });
  }

  function problemOf(outcome) {
    if (outcome.problem) {
      return outcome.problem;
    }
    return outcome.data.status === "ASSESSED" ? null : NOT_ASSESSED_MESSAGE;
  }

  // ---- The panel inside Swagger UI ---------------------------------------------------------------------------

  function panel(outcome, expanded, refresh) {
    var root = element("details", "bq-panel");
    root.open = expanded;
    var problem = problemOf(outcome);
    if (problem) {
      return append(root, [summaryLine(problem), element("p", "bq-note", problem)]);
    }
    return append(root, [scoreSummary(outcome.data), panelBody(outcome.data, refresh)]);
  }

  function summaryLine(text) {
    var summary = element("summary", "bq-summary");
    return append(summary, [element("span", "bq-title", "API assessment"), element("span", "bq-muted", text)]);
  }

  function scoreSummary(assessment) {
    var score = assessment.score;
    var summary = element("summary", "bq-summary");
    var facts = element("span", "bq-facts");
    append(facts, [
      element("span", "bq-title", "API maturity"),
      gradeBadge(score.grade, assessment.blockers),
      securityBadge(assessment.securityRating),
      blockerBadge(assessment.blockers),
      element("span", "bq-muted", score.assessedCategories + " of " + score.totalCategories + " categories assessed"),
      element("span", "bq-muted", plural(assessment.findings.length, "finding"))
    ]);
    return append(summary, [ring(score, ""), facts]);
  }

  function panelBody(assessment, refresh) {
    var columns = element("div", "bq-columns");
    var overview = append(element("div", "bq-stack"), [scoreCard(assessment, refresh), categories(assessment.categories)]);
    append(columns, [overview, findingsSection("Findings", assessment.findings, "No findings. Every rule passed.")]);
    var body = element("div", "bq-body");
    return append(body, [columns, buildEvidence(assessment), failedRules(assessment.failedRules)]);
  }

  // ---- Score ------------------------------------------------------------------------------------------------

  function ring(score, sizeClass) {
    var overall = score.overall;
    var wrapper = element("span", "bq-ring bq-grade-" + (score.grade || "none") + " " + sizeClass);
    var svgNs = "http://www.w3.org/2000/svg";
    var svg = document.createElementNS(svgNs, "svg");
    svg.setAttribute("viewBox", "0 0 36 36");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    [["bq-ring-track", 100], ["bq-ring-arc", overall === null ? 0 : overall]].forEach(function (part) {
      var circle = document.createElementNS(svgNs, "circle");
      circle.setAttribute("cx", "18");
      circle.setAttribute("cy", "18");
      circle.setAttribute("r", "15.5");
      circle.setAttribute("pathLength", "100");
      circle.setAttribute("class", part[0]);
      circle.setAttribute("stroke-dasharray", part[1] + " 100");
      svg.appendChild(circle);
    });
    return append(wrapper, [svg, ringNumber(overall, sizeClass), element("span", "bq-visually-hidden", ringLabel(score))]);
  }

  function ringNumber(overall, sizeClass) {
    var number = element("span", "bq-ring-number", overall === null ? "–" : overall);
    number.setAttribute("aria-hidden", "true");
    if (sizeClass && overall !== null) {
      number.appendChild(element("span", "bq-ring-out-of", "/ 100"));
    }
    return number;
  }

  function ringLabel(score) {
    return score.overall === null ? "No score" : "Score " + score.overall + " out of 100, grade " + score.grade;
  }

  // A blocker caps the grade at F whatever the score, so say why the two disagree.
  function gradeBadge(grade, blockers) {
    if (!grade) {
      return null;
    }
    var text = blockers > 0 ? "Grade " + grade + ", capped by blockers" : "Grade " + grade;
    return element("span", "bq-badge bq-grade-" + grade, text);
  }

  function securityBadge(rating) {
    return rating ? element("span", "bq-badge bq-security-" + rating, "Security " + rating) : null;
  }

  function blockerBadge(blockers) {
    return blockers > 0 ? element("span", "bq-badge bq-blocker", plural(blockers, "blocker")) : null;
  }

  function scoreCard(assessment, refresh) {
    var score = assessment.score;
    var head = element("div", "bq-score-head");
    var title = append(element("h3", "bq-heading"), [mark(""), document.createTextNode("API maturity")]);
    append(head, [title, refreshButton(refresh)]);
    var text = element("div", "bq-score-text");
    append(text, [
      element("p", "bq-grade-word bq-grade-" + (score.grade || "none"), gradeWord(score.grade)),
      element("p", "", verdict(assessment)),
      append(element("p", "bq-badges"), [gradeBadge(score.grade, assessment.blockers),
        securityBadge(assessment.securityRating), blockerBadge(assessment.blockers)]),
      element("p", "bq-muted bq-small", "Assessed " + age(assessment.generatedAt) + " · " + score.assessedCategories
        + " of " + score.totalCategories + " categories assessed")
    ]);
    var main = append(element("div", "bq-score-main"), [ring(score, "bq-ring-large"), text]);
    return append(element("section", "bq-score-card"), [head, main]);
  }

  function gradeWord(grade) {
    return grade ? GRADE_WORDS[grade] || "Grade " + grade : "No grade yet";
  }

  function verdict(assessment) {
    if (assessment.blockers > 0) {
      return assessment.blockers === 1
        ? "1 blocker caps the grade at F. Fix it first."
        : assessment.blockers + " blockers cap the grade at F. Fix them first.";
    }
    if (assessment.findings.length === 0) {
      return "Every rule passed.";
    }
    return plural(assessment.findings.length, "finding") + " to review, the most severe first.";
  }

  function refreshButton(refresh) {
    var button = element("button", "bq-button", "Refresh");
    button.type = "button";
    button.addEventListener("click", function () {
      button.disabled = true;
      button.textContent = "Refreshing…";
      refresh();
    });
    return button;
  }

  // ---- Categories ---------------------------------------------------------------------------------------------

  function categories(list) {
    var section = element("section", "bq-section");
    var tiles = element("ul", "bq-categories");
    list.forEach(function (category) {
      tiles.appendChild(categoryTile(category));
    });
    return append(section, [element("h3", "bq-heading", "Categories"), tiles]);
  }

  function categoryTile(category) {
    var tile = element("li", "bq-tile" + (category.assessed ? "" : " bq-tile-off"));
    var scored = category.assessed && category.score !== null;
    var value = category.assessed
      ? (scored ? category.score : "No score")
      : NOT_ASSESSED_REASONS[category.reason] || "Not assessed";
    return append(tile, [element("span", "bq-tile-value" + (scored ? " " + band(category.score) : ""), value),
      element("span", "bq-tile-name", CATEGORY_NAMES[category.name] || category.name)]);
  }

  function band(value) {
    if (value >= GOOD_FROM) {
      return "bq-good";
    }
    return value >= FAIR_FROM ? "bq-fair" : "bq-poor";
  }

  // ---- Build evidence: coverage, tests and vulnerabilities from the application's build reports -------------

  function buildEvidence(assessment) {
    var inputs = {};
    (assessment.inputs || []).forEach(function (input) { inputs[input.type] = input; });
    var testing = assessment.categories.filter(function (category) { return category.name === "TESTING"; })[0];
    var cards = element("div", "bq-cards");
    append(cards, [testingCard(inputs, (testing && testing.metrics) || {}),
      vulnerabilityCard(inputs.VULNERABILITIES, assessment.findings), securityCard(assessment.securityRating)]);
    var section = element("section", "bq-section");
    return append(section, [element("h3", "bq-heading", "Build evidence"), cards, reportAges(inputs)]);
  }

  function testingCard(inputs, metrics) {
    var coverageRead = inputs.COVERAGE && inputs.COVERAGE.status === "READ";
    var testsRead = inputs.TEST_RESULTS && inputs.TEST_RESULTS.status === "READ";
    var bars = element("div", "bq-bars");
    if (coverageRead) {
      bars.appendChild(bar("Line coverage", metrics.lineCoverage));
      if (metrics.branchCoverage !== undefined) {
        bars.appendChild(bar("Branch coverage", metrics.branchCoverage));
      }
    }
    var tests = testsRead ? element("p", "", metrics.testsPassed + " passed · " + metrics.testsFailed + " failed · "
      + metrics.testsSkipped + " skipped") : null;
    return card("Tests and coverage", [coverageRead ? bars : null, tests, hint(inputs.COVERAGE),
      hint(inputs.TEST_RESULTS)]);
  }

  function bar(label, percent) {
    var fill = element("div", "bq-bar-fill " + band(percent));
    fill.style.width = Math.max(0, Math.min(100, percent)) + "%";
    var track = append(element("div", "bq-bar"), [fill]);
    track.setAttribute("aria-hidden", "true");
    return append(element("div", "bq-bar-row"), [element("span", "", label), track,
      element("span", "bq-bar-value", percent + "%")]);
  }

  function vulnerabilityCard(input, findingList) {
    if (!input || input.status !== "READ") {
      return card("Known vulnerabilities", [hint(input)]);
    }
    var counts = element("ul", "bq-band-counts");
    var total = 0;
    VULNERABILITY_BANDS.forEach(function (vulnerabilityBand) {
      var count = findingList.filter(function (finding) { return finding.ruleId === vulnerabilityBand.ruleId; }).length;
      total += count;
      counts.appendChild(append(element("li", ""), [element("span", "bq-chip " + vulnerabilityBand.chip,
        vulnerabilityBand.label), element("span", "", count)]));
    });
    var summary = element("p", "bq-card-value",
      total === 0 ? "None found" : total + (total === 1 ? " advisory" : " advisories"));
    return card("Known vulnerabilities", [summary, counts]);
  }

  function securityCard(rating) {
    return card("Security rating", [element("p", "bq-card-value", rating || "Not rated"),
      element("p", "bq-muted bq-small", "From the security rules, worst finding first.")]);
  }

  function card(title, children) {
    var node = element("div", "bq-card");
    return append(node, [element("p", "bq-card-title", title)].concat(children));
  }

  function hint(input) {
    return input && input.status !== "READ" && input.hint ? element("p", "bq-note", input.hint) : null;
  }

  function reportAges(inputs) {
    var read = REPORT_INPUTS.map(function (type) { return inputs[type]; })
      .filter(function (input) { return input && input.file && input.modifiedAt; });
    if (read.length === 0) {
      return null;
    }
    var text = read.map(function (input) { return input.file + " written " + age(input.modifiedAt); }).join(" · ");
    return element("p", "bq-muted bq-report-ages", "Reports: " + text + ". Rebuild to refresh them.");
  }

  function age(timestamp) {
    var minutes = Math.floor((Date.now() - Date.parse(timestamp)) / 60000);
    if (isNaN(minutes) || minutes < 1) {
      return "just now";
    }
    if (minutes < 60) {
      return plural(minutes, "minute") + " ago";
    }
    var hours = Math.floor(minutes / 60);
    return hours < 48 ? plural(hours, "hour") + " ago" : plural(Math.floor(hours / 24), "day") + " ago";
  }

  function failedRules(list) {
    if (!list || list.length === 0) {
      return null;
    }
    var ids = list.map(function (failed) { return failed.ruleId; }).join(", ");
    return element("p", "bq-note", plural(list.length, "rule") + " could not run: " + ids + ".");
  }

  // ---- Findings, with filters -------------------------------------------------------------------------------

  function findingsSection(title, list, emptyText) {
    var section = element("section", "bq-section");
    var heading = element("h3", "bq-heading", title);
    heading.appendChild(element("span", "bq-count", list.length));
    section.appendChild(heading);
    if (list.length === 0) {
      return append(section, [element("p", "bq-note", emptyText)]);
    }
    var items = element("ul", "bq-findings");
    var filters = findingFilters(list, function (filter) { showFindings(items, list, filter); });
    showFindings(items, list, NO_FILTER);
    return append(section, [filters, items]);
  }

  function findingFilters(list, onChange) {
    var severity = choice("All severities", SEVERITIES.filter(function (entry) {
      return list.some(function (finding) { return finding.severity === entry.key; });
    }).map(function (entry) { return [entry.key, entry.label]; }));
    var category = choice("All categories",Object.keys(CATEGORY_NAMES).filter(function (name) {
      return list.some(function (finding) { return finding.category === name; });
    }).map(function (name) { return [name, CATEGORY_NAMES[name]]; }));
    var text = searchField("Search findings");
    var current = function () {
      onChange({ severity: severity.value, category: category.value, text: text.value.trim().toLowerCase() });
    };
    [severity, category].forEach(function (control) { control.addEventListener("change", current); });
    text.addEventListener("input", current);
    return append(element("div", "bq-filters"), [labelled("Severity", severity), labelled("Category", category),
      labelled("Search findings", text)]);
  }

  function choice(allText, options) {
    var select = element("select", "");
    select.appendChild(new Option(allText, ""));
    options.forEach(function (option) { select.appendChild(new Option(option[1], option[0])); });
    return select;
  }

  function searchField(placeholder) {
    var input = element("input", "");
    input.type = "search";
    input.placeholder = placeholder;
    return input;
  }

  function labelled(text, control) {
    var label = element("label", "");
    label.style.display = "contents";
    return append(label, [element("span", "bq-visually-hidden", text), control]);
  }

  function showFindings(items, list, filter) {
    items.textContent = "";
    var shown = list.filter(function (finding) { return matches(finding, filter); }).sort(bySeverity);
    shown.forEach(function (finding) { items.appendChild(findingItem(finding)); });
    if (shown.length === 0) {
      items.appendChild(element("li", "bq-note", "No findings match the filters."));
    }
  }

  function matches(finding, filter) {
    var text = (finding.ruleId + " " + finding.message + " " + finding.evidence.location).toLowerCase();
    return (!filter.severity || finding.severity === filter.severity)
      && (!filter.category || finding.category === filter.category)
      && (!filter.text || text.indexOf(filter.text) >= 0);
  }

  function bySeverity(first, second) {
    return severityIndex(first.severity) - severityIndex(second.severity);
  }

  function severityIndex(key) {
    return SEVERITIES.map(function (entry) { return entry.key; }).indexOf(key);
  }

  function findingItem(finding) {
    var severity = SEVERITIES[severityIndex(finding.severity)] || { key: finding.severity, label: finding.severity };
    var item = element("li", "bq-finding bq-edge-" + severity.key.toLowerCase());
    var heading = element("p", "bq-finding-heading");
    append(heading, [element("span", "bq-chip bq-" + severity.key.toLowerCase(), severity.label),
      element("span", "bq-rule-id", finding.ruleId), element("span", "", finding.message)]);
    var where = element("p", "bq-where");
    append(where, [element("span", "", describe(finding.evidence)),
      element("code", "bq-pointer", finding.evidence.location)]);
    return append(item, [heading, where, fix(finding)]);
  }

  function fix(finding) {
    var details = element("details", "bq-fix");
    append(details, [element("summary", "", "How to fix"), element("p", "", finding.remediation)]);
    if (typeof finding.docUrl === "string" && finding.docUrl.indexOf(RULE_PAGES) === 0) {
      var link = element("a", "", "Read about " + finding.ruleId);
      link.href = finding.docUrl;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      details.appendChild(link);
    }
    return details;
  }

  // Findings about changes since the baseline point into the document as well, like those about the document.
  function pointsIntoDocument(evidence) {
    return evidence.source === "OPENAPI" || evidence.source === "BASELINE";
  }

  // Turns "/paths/~1orders~1{id}/get/responses/404" into "GET /orders/{id} · responses 404".
  function describe(evidence) {
    if (!pointsIntoDocument(evidence)) {
      return EVIDENCE_SOURCES[evidence.source] || evidence.source;
    }
    var tokens = evidence.location.split("/").slice(1).map(function (token) {
      return token.replace(/~1/g, "/").replace(/~0/g, "~");
    });
    if (tokens[0] !== "paths" || tokens.length < 2) {
      return tokens.join(" ") || "Whole document";
    }
    var path = tokens[1];
    if (tokens.length < 3 || HTTP_METHODS.indexOf(tokens[2]) < 0) {
      return path;
    }
    var operation = tokens[2].toUpperCase() + " " + path;
    return tokens.length > 3 ? operation + " · " + tokens.slice(3).join(" ") : operation;
  }

  function plural(count, noun) {
    return count + " " + noun + (count === 1 ? "" : "s");
  }

  // ---- The dashboard page -----------------------------------------------------------------------------------

  function dashboard(root) {
    applyTheme(savedTheme());
    var model = { selected: "" };
    var refresh = function () {
      Promise.all([fetchJson("/assessment"), fetchJson("/openapi")]).then(function (results) {
        model.assessment = results[0];
        model.operations = operationsOf(results[1]);
        renderDashboard(root, model, refresh);
      });
    };
    refresh();
  }

  function renderDashboard(root, model, refresh) {
    var centre = element("main", "bq-centre");
    var select = function (key) {
      model.selected = key;
      centre.textContent = "";
      centre.appendChild(centreView(model));
      markCurrent(root, key);
    };
    var layout = append(element("div", "bq-layout"), [sidebar(model, select), centre, rightColumn(model, refresh)]);
    root.textContent = "";
    root.appendChild(append(element("div", "bq-panel bq-app"), [topBar(model), layout]));
    select(selectedKey(model));
  }

  function findingsOf(model) {
    return problemOf(model.assessment) ? [] : model.assessment.data.findings;
  }

  // A refresh keeps the selected operation only while the document still has it.
  function selectedKey(model) {
    var known = (model.operations || []).some(function (operation) { return operation.key === model.selected; });
    return known ? model.selected : "";
  }

  function topBar(model) {
    var bar = element("header", "bq-topbar");
    var title = element("div", "");
    var documentInfo = problemOf(model.assessment) ? null : model.assessment.data.document;
    var brand = append(element("h1", "bq-brand"), [mark("bq-mark-large"), element("span", "bq-wordmark", "Brekquater"),
      element("span", "bq-brand-divider"), element("span", "", "API maturity")]);
    append(title, [brand,
      documentInfo ? element("p", "bq-muted bq-small", documentInfo.title + " " + documentInfo.version) : null]);
    return append(bar, [title, themeButton()]);
  }

  function rightColumn(model, refresh) {
    var column = element("aside", "bq-right");
    column.setAttribute("aria-label", "Score and build evidence");
    if (problemOf(model.assessment)) {
      return column;
    }
    var assessment = model.assessment.data;
    return append(column, [scoreCard(assessment, refresh), categories(assessment.categories),
      buildEvidence(assessment), failedRules(assessment.failedRules)]);
  }

  // ---- Dashboard: the operation list ------------------------------------------------------------------------

  function operationsOf(outcome) {
    if (!outcome.data || !outcome.data.paths) {
      return null;
    }
    var operations = [];
    Object.keys(outcome.data.paths).forEach(function (path) {
      var pathItem = outcome.data.paths[path] || {};
      HTTP_METHODS.filter(function (method) { return pathItem[method]; }).forEach(function (method) {
        operations.push(operation(path, method, pathItem[method]));
      });
    });
    return operations;
  }

  function operation(path, method, details) {
    return {
      key: "/paths/" + pointerToken(path) + "/" + method,
      path: path,
      method: method,
      summary: details.summary || details.operationId || "",
      tag: (details.tags && details.tags[0]) || "Other"
    };
  }

  function pointerToken(text) {
    return text.replace(/~/g, "~0").replace(/\//g, "~1");
  }

  function findingsAt(findings, key) {
    return findings.filter(function (finding) {
      var location = finding.evidence.location;
      return pointsIntoDocument(finding.evidence) && (location === key || location.indexOf(key + "/") === 0);
    });
  }

  function sidebar(model, select) {
    var nav = element("nav", "bq-sidebar");
    nav.setAttribute("aria-label", "Operations");
    var findings = findingsOf(model);
    var overview = navItem("", null, "Overview", findings.length, select);
    if (!model.operations) {
      return append(nav, [navList([overview]), element("p", "bq-note", "The operation list is not available.")]);
    }
    var search = searchField("Filter operations");
    search.className = "bq-search";
    var groups = tagGroups(model.operations, findings, select);
    search.addEventListener("input", function () { filterOperations(groups, search.value.trim().toLowerCase()); });
    return append(nav, [labelled("Filter operations", search), navList([overview])].concat(groups));
  }

  function tagGroups(operations, findings, select) {
    var tags = [];
    operations.forEach(function (entry) {
      if (tags.indexOf(entry.tag) < 0) {
        tags.push(entry.tag);
      }
    });
    return tags.map(function (tag) {
      var members = operations.filter(function (entry) { return entry.tag === tag; });
      return tagGroup(tag, members.map(function (entry) {
        return navItem(entry.key, entry, entry.path, findingsAt(findings, entry.key).length, select);
      }));
    });
  }

  function tagGroup(tag, items) {
    var group = element("details", "bq-nav-group");
    group.open = true;
    var summary = element("summary", "bq-group-summary");
    append(summary, [element("span", "bq-group-name", tag), element("span", "bq-count", items.length)]);
    return append(group, [summary, navList(items)]);
  }

  function navList(items) {
    return append(element("ul", "bq-nav-list"), items);
  }

  function navItem(key, entry, text, findingCount, select) {
    var button = element("button", "bq-nav-item");
    button.type = "button";
    button.setAttribute("data-key", key);
    button.setAttribute("data-search", ((entry ? entry.summary : "") + " " + text).toLowerCase());
    append(button, [entry ? methodPill(entry.method) : null, element("span", "bq-nav-path", text),
      findingCount > 0 ? element("span", "bq-count", findingCount) : null]);
    if (findingCount > 0) {
      button.setAttribute("aria-label", text + ", " + plural(findingCount, "finding"));
    }
    button.addEventListener("click", function () { select(key); });
    return append(element("li", ""), [button]);
  }

  function methodPill(method) {
    return element("span", "bq-method bq-method-" + method, method.toUpperCase());
  }

  function filterOperations(groups, text) {
    groups.forEach(function (group) {
      var visible = 0;
      Array.prototype.forEach.call(group.querySelectorAll("li"), function (item) {
        var shown = !text || item.firstChild.getAttribute("data-search").indexOf(text) >= 0;
        item.hidden = !shown;
        visible += shown ? 1 : 0;
      });
      group.hidden = visible === 0;
    });
  }

  function markCurrent(root, key) {
    Array.prototype.forEach.call(root.querySelectorAll(".bq-nav-item"), function (button) {
      if (button.getAttribute("data-key") === key) {
        button.setAttribute("aria-current", "true");
      } else {
        button.removeAttribute("aria-current");
      }
    });
  }

  // ---- Dashboard: the centre view ---------------------------------------------------------------------------

  function centreView(model) {
    var view = element("div", "bq-stack");
    var problem = problemOf(model.assessment);
    if (problem) {
      return append(view, [viewHead(null, "API assessment", problem)]);
    }
    var findings = model.assessment.data.findings;
    var selected = (model.operations || []).filter(function (entry) { return entry.key === model.selected; })[0];
    if (!selected) {
      return append(view, [overviewHead(model.assessment.data.document),
        findingsSection("All findings", findings, "No findings. Every rule passed.")]);
    }
    return append(view, [operationHead(selected),
      findingsSection("Findings", findingsAt(findings, selected.key), "No findings for this operation.")]);
  }

  function overviewHead(documentInfo) {
    var facts = "Version " + documentInfo.version + " · " + plural(documentInfo.operations, "operation")
      + " · OpenAPI " + documentInfo.openapi;
    return viewHead("Overview", documentInfo.title || "API", facts);
  }

  function operationHead(entry) {
    var head = viewHead("Overview › " + entry.tag, null, entry.summary || null);
    var title = head.querySelector(".bq-view-title");
    append(title, [methodPill(entry.method), element("code", "", entry.path)]);
    return head;
  }

  function viewHead(crumb, title, text) {
    var head = element("section", "bq-view-head");
    return append(head, [crumb ? element("p", "bq-crumb", crumb) : null, element("h2", "bq-view-title", title),
      text ? element("p", "bq-muted", text) : null]);
  }

  // ---- Dashboard: theme switch ------------------------------------------------------------------------------

  // Browser storage can be missing or refuse access (private windows, blocked site data), so it is only a
  // convenience: without it the page starts in the "System" theme.
  function savedTheme() {
    try {
      var saved = window.localStorage.getItem(THEME_KEY);
      return THEMES.indexOf(saved) >= 0 ? saved : THEMES[0];
    } catch (error) {
      return THEMES[0];
    }
  }

  function applyTheme(theme) {
    if (theme === "system") {
      document.documentElement.removeAttribute("data-theme");
    } else {
      document.documentElement.setAttribute("data-theme", theme);
    }
    try {
      window.localStorage.setItem(THEME_KEY, theme);
    } catch (error) {
      // Not remembered; the switch still works for this visit.
    }
  }

  function themeButton() {
    var button = element("button", "bq-button");
    button.type = "button";
    var show = function (theme) {
      button.textContent = "Theme: " + THEME_NAMES[theme];
      button.setAttribute("data-theme-choice", theme);
    };
    show(savedTheme());
    button.addEventListener("click", function () {
      var next = THEMES[(THEMES.indexOf(button.getAttribute("data-theme-choice")) + 1) % THEMES.length];
      applyTheme(next);
      show(next);
    });
    return button;
  }

  // ---- Placing the panel ------------------------------------------------------------------------------------

  // On wide screens the stylesheet turns the panel into a fixed column beside Swagger UI, and Swagger UI's page
  // gets room for it. The panel stays where it was inserted: Swagger UI's elements belong to React and are never
  // moved, which keeps the operations working. In the column the panel is always open.
  function injectIntoSwaggerUi(outcome) {
    whenSwaggerUiIsReady(function (anchor) {
      var wrapper = element("div", "bq-swagger-wrapper");
      anchor.parentNode.insertBefore(wrapper, anchor.nextSibling);
      document.body.classList.add("bq-has-aside");
      var render = function (latest, expanded) {
        wrapper.textContent = "";
        wrapper.appendChild(panel(latest, expanded || ASIDE.matches, refresh));
      };
      var refresh = function () {
        fetchJson("/assessment").then(function (latest) { render(latest, true); });
      };
      ASIDE.addEventListener("change", function (change) {
        if (change.matches && wrapper.firstChild) {
          wrapper.firstChild.open = true;
        }
      });
      render(outcome, false);
    });
  }

  // Swagger UI renders after the page loads, so wait for its information block. If it never appears, for example
  // because a Swagger UI upgrade changed its markup, show the panel at the top of the page instead.
  function whenSwaggerUiIsReady(place) {
    var found = document.querySelector(SWAGGER_UI_ANCHOR);
    if (found) {
      place(found);
      return;
    }
    var observer = new MutationObserver(function () {
      var anchor = document.querySelector(SWAGGER_UI_ANCHOR);
      if (anchor) {
        observer.disconnect();
        clearTimeout(fallback);
        place(anchor);
      }
    });
    var fallback = setTimeout(function () {
      observer.disconnect();
      var top = element("div", "bq-top-anchor");
      document.body.insertBefore(top, document.body.firstChild);
      place(top);
    }, SWAGGER_UI_WAIT_MS);
    observer.observe(document.body, { childList: true, subtree: true });
  }

  var standaloneRoot = document.getElementById(STANDALONE_ROOT_ID);
  if (standaloneRoot) {
    dashboard(standaloneRoot);
  } else {
    fetchJson("/assessment").then(injectIntoSwaggerUi);
  }
})();
