(function (widget, zoomableSunburst, utils, jobColors, dataSource) {
    var diameter = 600;

    var svg = widget.create("Average test runtime",
                            "Color: Job/Test Suite, Arc size: duration",
                            "/testclasses.csv")
            .svg(diameter);

    var graph = zoomableSunburst(svg, diameter);

    var title = function (entry) {
        return entry.name + ' (' + utils.formatTimeInMs(entry.size, {showMillis: true}) + ')';
    };

    var hasOnlyOneChild = function (children) {
        return children && children.length === 1;
    };

    var skipOnlyTestSuite = function (children) {
        var hasOnlyOneTestSuite = hasOnlyOneChild(children);

        return hasOnlyOneTestSuite ? children[0].children : children;
    };

    var buildNodeStructure = function (hierarchy) {
        return Object.keys(hierarchy).map(function (nodeName) {
            var entry = hierarchy[nodeName];

            if (entry.name) {
                return {
                    name: nodeName,
                    averageRuntime: entry.averageRuntime
                };
            } else {
                return {
                    name: nodeName,
                    children: buildNodeStructure(entry)
                };
            }
        });
    };

    var buildPackageHierarchy = function (classEntries) {
        var packageHierarchy = {};

        classEntries.forEach(function (entry) {
            var packageClassName = entry.name,
                components = packageClassName.split('.'),
                packagePath = components.slice(0, -1),
                className = components.pop();

            var branch = packagePath.reduce(function (packageBranch, packageName) {
                if (!packageBranch[packageName]) {
                    packageBranch[packageName] = {};
                }
                return packageBranch[packageName];
            }, packageHierarchy);

            branch[className] = entry;
        });

        return buildNodeStructure(packageHierarchy);
    };

    var transformClassNode = function (elem) {
        var children = elem.children && elem.children.map(transformClassNode);

        if (children) {
            if (children.length === 1 && children[0].children) {
                return {
                    name: elem.name + '.' + children[0].name,
                    children: children[0].children
                };
            } else {
                return {
                    name: elem.name,
                    children: children
                };
            }
        } else {
            return {
                name: elem.name,
                size: elem.averageRuntime
            };
        }
    };

    var addAccumulatedApproximateRuntime = function (elem) {
        if (elem.children) {
            elem.children = elem.children.map(addAccumulatedApproximateRuntime);
            elem.size = elem.children.reduce(function (acc, child) {
                return acc + child.size;
            }, 0);
        }
        return elem;
    };

    var addTitle = function (elem) {
        elem.title = title(elem);
        if (elem.children) {
            elem.children = elem.children.map(addTitle);
        }
        return elem;
    };

    var transformClasses = function (classNodes) {
        return buildPackageHierarchy(classNodes)
            .map(transformClassNode)
            .map(addAccumulatedApproximateRuntime)
            .map(addTitle);
    };

    var transformTestSuite = function (node) {
        if (!node.children) {
            return transformClasses([node])[0];
        }

        var classNodes = node.children.filter(function (child) {
            return !child.children;
        });

        var nestedSuites = node.children.filter(function (child) {
            return child.children;
        });

        return {
            name: node.name,
            children: transformClasses(classNodes).concat(nestedSuites.map(transformTestSuite))
        };
    };

    var skipParentNodesIfAllOnlyHaveOneChild = function (nodes) {
        var allHaveOneChild = nodes.reduce(function (allHaveOneChild, node) {
            return allHaveOneChild && hasOnlyOneChild(node);
        }, true);

        if (allHaveOneChild) {
            return skipParentNodesIfAllOnlyHaveOneChild(nodes.map(function (node) {
                return node.children[0];
            }));
        } else {
            return nodes;
        }
    };

    var transformTestsuites = function (jobMap) {
        var jobNames = Object.keys(jobMap),
            color = jobColors.colors(jobNames);

        return jobNames
            .filter(function (jobName) {
                return jobMap[jobName].children.length > 0;
            })
            .map(function (jobName) {
                var job = jobMap[jobName],
                    children = job.children;

                return {
                    name: jobName,
                    color: color(jobName),
                    id: 'jobname-' + jobName,
                    children: skipParentNodesIfAllOnlyHaveOneChild(skipOnlyTestSuite(children.map(transformTestSuite)))
                };
            });
    };

    var timestampOneWeekAgo = function () {
        var today = new Date(),
            oneWeekAgo = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 7);
        return +oneWeekAgo;
    };

    dataSource.load('/testclasses?from='+ timestampOneWeekAgo(), function (testsuites) {
        var data = {
            name: "Testsuites",
            children: transformTestsuites(testsuites)
        };

        graph.render(data);
    });
}(widget, zoomableSunburst, utils, jobColors, dataSource));
