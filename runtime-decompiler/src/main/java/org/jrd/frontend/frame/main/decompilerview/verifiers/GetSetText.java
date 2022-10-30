package org.jrd.frontend.frame.main.decompilerview.verifiers;

public interface GetSetText {

    String getText();

    void setText(String s);

    class DummyGetSet implements GetSetText {
        private String text;

        public DummyGetSet(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public void setText(String s) {
            text = s;
        }
    }

}
