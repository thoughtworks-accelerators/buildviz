(ns buildviz.junit-xml-test
  (:use clojure.test)
  (:require [buildviz.junit-xml :as junit-xml]))

(deftest Parsing
  (testing "parse-testsuites"
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :fail}]}]
           (junit-xml/parse-testsuites "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"><failure/></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :error}]}]
           (junit-xml/parse-testsuites "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"><error/></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass}]}]
           (junit-xml/parse-testsuites "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :classname "the class"
                         :status :fail}]}]
       (junit-xml/parse-testsuites "<testsuites><testsuite name=\"a suite\"><testcase classname=\"the class\" name=\"a test\"><failure/></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a sub suite"
                         :children [{:name "a test"
                                     :status :pass}]}]}]
           (junit-xml/parse-testsuites "<testsuites><testsuite name=\"a suite\"><testsuite name=\"a sub suite\"><testcase name=\"a test\"></testcase></testsuite></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass}]}
             {:name "another suite"
              :children [{:name "another test"
                          :status :pass}]}]
           (junit-xml/parse-testsuites "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"></testcase></testsuite><testsuite name=\"another suite\"><testcase name=\"another test\"></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass}]}]
           (junit-xml/parse-testsuites "<testsuite name=\"a suite\"><testcase name=\"a test\"></testcase></testsuite>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass
                         :runtime 1234}]}]
           (junit-xml/parse-testsuites "<testsuite name=\"a suite\"><testcase name=\"a test\" time=\"1.234\"></testcase></testsuite>")))
    ))
