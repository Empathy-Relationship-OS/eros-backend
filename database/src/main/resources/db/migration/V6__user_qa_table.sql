-- V6: Questions and user answers table
-- Holds information for the user Q&A answers linking to the question..
-- question_id links to the question the answer relates to.
-- user_id is the id of the user the answer relates to.
-- display_order is the order the Q&A is shown on the users profile.

CREATE TABLE IF NOT EXISTS user_qa(

    -- Foreign key.
    question_id BIGSERIAL REFERENCES questions(question_id) ON DELETE CASCADE,
    user_id varchar(128) NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,

    answer varchar(200) NOT NULL,
    display_order INTEGER NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    PRIMARY KEY (user_id, question_id),

    -- Constraints
    CONSTRAINT user_and_question_unique UNIQUE (user_id, question_id),
    CONSTRAINT user_and_order_unique UNIQUE (user_id, display_order)
);