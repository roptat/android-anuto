package ch.logixisland.anuto.business.level;

import java.util.ArrayList;
import java.util.List;

import ch.logixisland.anuto.business.score.ScoreBoard;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.entity.enemy.Enemy;
import ch.logixisland.anuto.entity.enemy.EnemyFactory;
import ch.logixisland.anuto.entity.enemy.EnemyListener;
import ch.logixisland.anuto.util.data.EnemyDescriptor;
import ch.logixisland.anuto.util.data.WaveDescriptor;
import ch.logixisland.anuto.util.math.MathUtils;
import ch.logixisland.anuto.util.math.Vector2;

class EnemyInserter implements EnemyListener {

    private final GameEngine mGameEngine;
    private final ScoreBoard mScoreBoard;
    private final EnemyFactory mEnemyFactory;
    private final WaveManager mWaveManager;
    private final WaveDescriptor mWaveDescriptor;

    private final List<Enemy> mRemainingEnemies = new ArrayList<>();

    private int mExtend;
    private int mWaveReward;
    private float mEnemyHealthModifier;
    private float mEnemyRewardModifier;

    EnemyInserter(GameEngine gameEngine, ScoreBoard scoreBoard, EnemyFactory enemyFactory,
                  WaveManager waveManager, WaveDescriptor waveDescriptor, int extend) {
        mGameEngine = gameEngine;
        mScoreBoard = scoreBoard;
        mEnemyFactory = enemyFactory;
        mWaveManager = waveManager;
        mWaveDescriptor = waveDescriptor;

        mExtend = extend;
        mEnemyHealthModifier = 1;
        mEnemyRewardModifier = 1;
        mWaveReward = mWaveDescriptor.getWaveReward();
    }

    int getExtend() {
        return mExtend;
    }

    float getEnemyHealthModifier() {
        return mEnemyHealthModifier;
    }

    float getEnemyRewardModifier() {
        return mEnemyRewardModifier;
    }

    WaveDescriptor getWaveDescriptor() {
        return mWaveDescriptor;
    }

    void modifyEnemyHealth(float modifier) {
        mEnemyHealthModifier *= modifier;
    }

    void modifyEnemyReward(float modifier) {
        mEnemyRewardModifier *= modifier;
    }

    void modifyWaveReward(float modifier) {
        mWaveReward *= modifier;
    }

    void start() {
        scheduleEnemies();
    }

    @Override
    public void enemyKilled(Enemy enemy) {
        mScoreBoard.giveCredits(enemy.getReward(), true);
    }

    @Override
    public void enemyFinished(Enemy enemy) {
        mScoreBoard.takeLives(1);
    }

    @Override
    public void enemyRemoved(Enemy enemy) {
        mRemainingEnemies.remove(enemy);
        mWaveManager.enemyRemoved();

        if (getRemainingEnemiesCount() == 0) {
            giveWaveReward();
            mWaveManager.waveFinished(this);
        }
    }

    void giveWaveReward() {
        mScoreBoard.giveCredits(mWaveReward, true);
        mWaveReward = 0;
    }

    float getRemainingEnemiesReward() {
        float totalReward = 0f;

        for (Enemy enemy : mRemainingEnemies) {
            totalReward += enemy.getReward();
        }

        return totalReward;
    }

    int getRemainingEnemiesCount() {
        return mRemainingEnemies.size();
    }

    private void scheduleEnemies() {
        int delay = 0;
        Vector2 offset = new Vector2();

        List<EnemyDescriptor> enemyDescriptors = mWaveDescriptor.getEnemies();

        for (int extendIndex = 0; extendIndex < mExtend + 1; extendIndex++) {
            for (EnemyDescriptor descriptor : enemyDescriptors) {
                if (MathUtils.equals(descriptor.getDelay(), 0f, 0.1f)) {
                    offset = offset.add(descriptor.getOffset());
                } else {
                    descriptor.getOffset();
                }

                Enemy enemy = createAndConfigureEnemy(offset, descriptor);

                if (extendIndex > 0 || enemyDescriptors.indexOf(descriptor) > 0) {
                    delay += (int) descriptor.getDelay();
                }

                addEnemy(enemy, delay);
            }
        }
    }

    private Enemy createAndConfigureEnemy(Vector2 offset, EnemyDescriptor descriptor) {
        final Enemy enemy = mEnemyFactory.createEnemy(descriptor.getName());
        enemy.modifyHealth(mEnemyHealthModifier);
        enemy.modifyReward(mEnemyRewardModifier);
        enemy.setPathIndex(descriptor.getPathIndex());
        enemy.move(offset);
        return enemy;
    }

    private void addEnemy(final Enemy enemy, int delay) {
        mRemainingEnemies.add(enemy);
        enemy.addListener(this);

        mGameEngine.postDelayed(new Runnable() {
            @Override
            public void run() {
                mGameEngine.add(enemy);
            }
        }, delay);
    }
}
